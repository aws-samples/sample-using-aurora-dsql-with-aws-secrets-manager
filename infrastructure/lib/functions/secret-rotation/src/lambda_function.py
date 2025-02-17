# Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

import json
import logging
import os
import tempfile

import boto3
import psycopg
import requests

logger = logging.getLogger()
logger.setLevel(os.environ.get("LOG_LEVEL", "INFO"))

# Set up the client
region = os.environ["AWS_REGION"]

session = boto3.Session(region_name=region)
secrets_manager_client = session.client("secretsmanager")
dsql_client = session.client("dsql")


class GenerateAuthToken:
    """
    GenerateAuthToken class to generate Aurora DSQL login tokens.
    """

    def __init__(self, host, expires_in_secs=900):
        """
        Initialize the token generator with the given parameters.
        :param host: The Aurora DSQL endpoint host name.
        :param expires_in_secs: The token expiry duration in seconds (default is 900 seconds).
        """
        self.host = host
        self.region = region
        self.expires_in_secs = expires_in_secs

    def generate_auth_token(self, username):
        """
        Generate an Aurora DSQL database token for the given username.
        :param username: If admin, invoke generate_db_connect_admin_auth_token else generate_db_connect_auth_token
        :return: A pre-signed url which can be used as an auth token.
        """
        if not username:
            raise ValueError("Username cannot be empty")
        username = username.strip().lower()
        if username == "admin":
            return dsql_client.generate_db_connect_admin_auth_token(
                self.host, self.region, self.expires_in_secs
            )
        else:
            return dsql_client.generate_db_connect_auth_token(
                self.host, self.region, self.expires_in_secs
            )


def lambda_handler(event, context):
    """Secrets Manager Aurora DSQL Handler

    This handler rotates the Aurora DSQL token

    The Secret SecretString is expected to be a JSON string with the following format:
    {
        'username': <required: username>,
        'password': <required: password>,
        'host': <required: instance host name>,
        'engine': <optional: default is set as 'postgres'>,
        'port': <optional: if not specified, default port 5432 will be used>,
        'dbname': <optional: if not specified, default database postgres will be used>
    }

    """
    arn = event["SecretId"]
    token = event["ClientRequestToken"]
    step = event["Step"]

    # Make sure the version is staged correctly
    metadata = secrets_manager_client.describe_secret(SecretId=arn)
    if "RotationEnabled" in metadata and not metadata["RotationEnabled"]:
        logger.error(f"Secret {arn} is not enabled for rotation")
        raise ValueError(f"Secret {arn} is not enabled for rotation")
    versions = metadata["VersionIdsToStages"]
    if token not in versions:
        logger.error(
            f"Secret version {token} has no stage for rotation of secret {arn}."
        )
        raise ValueError(
            f"Secret version {token} has no stage for rotation of secret {arn}."
        )
    if "AWSCURRENT" in versions[token]:
        logger.info(
            f"Secret version {token} already set as AWSCURRENT for secret {token}."
        )
        return
    elif "AWSPENDING" not in versions[token]:
        logger.error(
            f"Secret version {token} not set as AWSPENDING for rotation of secret {arn}."
        )
        raise ValueError(
            f"Secret version {token} not set as AWSPENDING for rotation of secret {arn}."
        )

    # Call the appropriate step
    if step == "createSecret":
        create_secret(arn, token)
    elif step == "setSecret":
        set_secret(arn, token)
    elif step == "testSecret":
        test_secret(arn, token)
    elif step == "finishSecret":
        finish_secret(arn, token)
    else:
        logger.error(f"lambda_handler: Invalid step parameter {step} for secret {arn}")
        raise ValueError(f"Invalid step parameter {step} for secret {arn}")


def create_secret(arn, token):
    # Make sure the current secret exists
    current_dict = get_secret_dict(arn, "AWSCURRENT")

    # Now try to get the secret version, if that fails, put a new secret
    try:
        get_secret_dict(arn, "AWSPENDING", token)
        logger.info(f"createSecret: Successfully retrieved secret for {arn}.")
    except secrets_manager_client.exceptions.ResourceNotFoundException:
        # Generate a random password with an expiry of 8 hours i.e. 28800 seconds
        token_generator = GenerateAuthToken(current_dict["host"], 28800)
        current_dict["password"] = token_generator.generate_auth_token(
            current_dict["username"]
        )
        # Put the secret
        secrets_manager_client.put_secret_value(
            SecretId=arn,
            ClientRequestToken=token,
            SecretString=json.dumps(current_dict),
            VersionStages=["AWSPENDING"],
        )
        logger.info(
            f"createSecret: Successfully put secret for ARN {arn} and version {token}."
        )


def set_secret(arn, token):
    logger.info(
        f"setSecret: Aurora DSQL does not require setting a secret for ARN {arn} and version {token}."
    )


def test_secret(arn, token):
    # Try to log in with the pending secret, if it succeeds, return
    conn = get_connection(get_secret_dict(arn, "AWSPENDING", token))
    if conn:
        # This is where the lambda will validate the user's permissions. Uncomment/modify the below lines to
        # tailor these validations to your needs
        try:
            with conn.cursor() as cur:
                cur.execute("SELECT CURRENT_TIMESTAMP")
                conn.commit()
        finally:
            conn.close()

        logger.info(
            f"testSecret: Successfully tested Aurora DSQL DB authentication with AWSPENDING secret in {arn}."
        )
        return
    else:
        logger.error(
            f"testSecret: Unable to log into database with pending secret of secret ARN {arn}"
        )
        raise ValueError(
            f"Unable to log into database with pending secret of secret ARN {arn}"
        )


def finish_secret(arn, token):
    # First describe the secret to get the current version
    metadata = secrets_manager_client.describe_secret(SecretId=arn)
    current_version = None
    for version in metadata["VersionIdsToStages"]:
        if "AWSCURRENT" in metadata["VersionIdsToStages"][version]:
            if version == token:
                # The correct version is already marked as current, return
                logger.info(
                    f"finishSecret: Version {version} already marked as AWSCURRENT for {arn}"
                )
                return
            current_version = version
            break

    # Finalize by staging the secret version current
    secrets_manager_client.update_secret_version_stage(
        SecretId=arn,
        VersionStage="AWSCURRENT",
        MoveToVersionId=token,
        RemoveFromVersionId=current_version,
    )
    logger.info(
        f"finishSecret: Successfully set AWSCURRENT stage to version {token} for secret {arn}."
    )


def get_secret_dict(arn, stage, token=None):
    required_fields = ["username", "password", "host"]
    # Only do VersionId validation against the stage if a token is passed in
    if token:
        secret = secrets_manager_client.get_secret_value(
            SecretId=arn, VersionId=token, VersionStage=stage
        )
    else:
        secret = secrets_manager_client.get_secret_value(
            SecretId=arn, VersionStage=stage
        )
    plaintext = secret["SecretString"]
    secret_dict = json.loads(plaintext)

    # Run validations against the secret
    supported_engines = ["postgres"]
    if "engine" not in secret_dict or secret_dict["engine"] not in supported_engines:
        raise KeyError(
            "Database engine must be set to 'postgres' in order to use this rotation lambda"
        )
    for field in required_fields:
        if field not in secret_dict:
            raise KeyError(f"{field} key is missing from secret JSON")

    # Parse and return the secret JSON string
    return secret_dict


def get_connection(secret_dict):
    # Parse and validate the secret JSON string
    port = int(secret_dict["port"]) if "port" in secret_dict else 5432
    dbname = secret_dict["dbname"] if "dbname" in secret_dict else "postgres"
    return connect_and_authenticate(secret_dict, port, dbname)


def get_amazon_root_cert():
    # Create a temporary file to store the certificate
    cert_file = tempfile.NamedTemporaryFile(delete=False)
    try:
        # Download the certificate
        response = requests.get(
            "https://www.amazontrust.com/repository/AmazonRootCA1.pem", timeout=30
        )
        cert_file.write(response.content)
        cert_file.close()
        return cert_file.name
    except Exception as e:
        if cert_file:
            cert_file.close()
            os.unlink(cert_file.name)
        raise e


def connect_and_authenticate(secret_dict, port, dbname):
    # Try to obtain a connection to the db
    cert_path = get_amazon_root_cert()
    pg_connection_dict = {
        "dbname": dbname,
        "user": secret_dict["username"],
        "password": secret_dict["password"],
        "port": port,
        "host": secret_dict["host"],
    }
    try:
        verify_full_dict = pg_connection_dict.copy()
        verify_full_dict["sslmode"] = "verify-full"
        verify_full_dict["sslrootcert"] = cert_path
        conn = psycopg.connect(**verify_full_dict)
        logger.info(
            f"Successfully established connection with verify-full as user {secret_dict['username']} with host: {secret_dict['host']}"
        )
        return conn
    except psycopg.Error as e:
        logger.error(
            f"Unable to establish connection as user {secret_dict['username']} with host: {secret_dict['host']}"
        )
        logger.error(e)
        # Fall back to require
        try:
            require_dict = pg_connection_dict.copy()
            require_dict["sslmode"] = "require"
            conn = psycopg.connect(**require_dict)
            logger.info(
                f"Successfully established connection with require as user {secret_dict['username']} with host: {secret_dict['host']}"
            )
            return conn
        except psycopg.Error as e:
            logger.error(
                f"Unable to establish connection as user {secret_dict['username']} with host: {secret_dict['host']}"
            )
            return None
    finally:
        # Clean up the temporary certificate file
        if cert_path and os.path.exists(cert_path):
            os.unlink(cert_path)
