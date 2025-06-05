# Amazon Aurora DSQL with AWS Secrets Manager

## Archive Notice
This project is being archived as of June 2025. The application has been tested using CDK `2.1017.0` and NodeJS `v22.16.0`

## Overview

Ephemeral tokens are temporary security credentials that are valid for a limited time period. Unlike long-lived credentials (such as IAM user access keys), ephemeral tokens automatically expire after a short duration and need to be refreshed or regenerated. The primary security benefit of ephemeral tokens is that they follow the principle of least privilege in the time dimension - credentials exist only as long as needed for specific operations, reducing the attack surface if credentials are leaked or stolen. In many architectures, ephemeral tokens are generated at runtime when needed rather than being stored centrally. 

> [!TIP]
> For generating the token at runtime, refer the [DSQL examples](https://github.com/aws-samples/aurora-dsql-samples/) on how to integrate token generation into several ORMs.

> [!WARNING]
> This project demonstrates an integration of AWS Secrets Manager with Amazon Aurora DSQL using AWS CDK and Java application. However, it should be noted that this approach - using centralized secret management instead of app-specific runtime ephemeral token generation - is **implemented solely for customers with legacy secret management restrictions impacting their use of Aurora DSQL** and is **NOT considered a best practice**.

> [!CAUTION]
> This method should be used with extreme caution, as **ephemeral credential generation provides superior security posture. AWS Secrets Manager for storing ephemeral tokens isn't optimal.** Ephemeral tokens are designed to be short-lived credentials. Generating a new ephemeral token for each connection is faster and less effort than using AWS Secrets Manager.

## Threat model
When considering the threat model for your architecture using Amazon Aurora DSQL with ephemeral tokens stored in AWS Secrets Manager and auto-rotated by AWS Lambda, consider the below potential security risks and vulnerabilities.

### Token Storage and Management Threats
- **Secret Exposure**: If the Secrets Manager secret is accessed by unauthorized users or services
- **Rotation Failures**: If the Lambda rotation function fails, tokens could expire leading to application downtime
- **Permission Escalation**: Overly permissive IAM roles for Lambda or application could allow unauthorized access

### Network and Communication Threats
- **Man-in-the-Middle Attacks**: Interception of tokens during transmission between services
- **Network Sniffing**: Capturing tokens in transit if not using encrypted connections
- **Service API Abuse**: Unauthorized API calls to Secrets Manager or Aurora DSQL endpoints

### Application-Level Threats
- **Token Leakage**: Application logs or error messages exposing tokens
- **Memory Dumps**: Tokens stored in application memory could be exposed through memory dumps
- **Insecure Token Handling**: Improper token storage or caching in application code

### Infrastructure Threats
- **Lambda Code Vulnerabilities**: Security flaws in the rotation Lambda function
- **Dependency Vulnerabilities**: Compromised libraries used by Lambda or application
- **Misconfigured IAM Permissions**: Over-permissive policies allowing unintended access

### Mitigation Recommendations
1. Use least-privilege IAM policies/resource policies for AWS Secrets Manager, AWS Lambda and other resources. This includes strict IAM policies limiting which identities can generate tokens using the `dsql:DbConnect` or `dsql:DbConnectAdmin` permissions
2. Implement VPC endpoints to keep traffic private
3. Enable encryption in transit and at rest for all components
4. Implement monitoring and alerting for token usage/ failures
5. Use AWS CloudTrail to audit access to secrets
6. Implement proper error handling to prevent token leakage
7. Implement application-level logging that doesn't expose sensitive information

## Architecture

![Architecture](./architecture.jpg)

## Pre-requisites

1. Create an Amazon Aurora DSQL cluster, we are using AWS region `us-east-1` for this example
2. Execute the [schema.sql](demo-app/schema.sql) on the newly created Amazon Aurora DSQL cluster
3. The project has been built and tested using below configurations on `x86` architecture

   a. Java - `v17.0.14` and Gradle - `v8.13`

    ```shell
    # Install SDKMAN - https://sdkman.io/install/
    curl -s "https://get.sdkman.io" | bash
    sdk install java 17.0.14-amzn
    sdk install gradle 8.13
    ```
   b. Python - `v3.13`

    ```shell
    # Install pyenv
    curl https://pyenv.run | bash

    # Install Python 3.13
    pyenv install 3.13
    pyenv global 3.13
    ```

   c. AWS CDK - `v2.1017.0`
    ```shell
    # Install NVM
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash

    # Install Node LTS
    nvm install --lts

    # Install AWS CDK
    npm install -g aws-cdk@2.1017.0
    ```

   d. Install `curl` and `jq`
    ```shell
    sudo dnf install curl jq
    ```

   e. Install [Docker](https://docs.docker.com/get-started/get-docker/) for building containers
  
   f. Install [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

## Deployment

1. Refer the [CDK infrastructure stack](infrastructure/README.md) to set up secret rotation and store the credentials in AWS Secrets Manager for Amazon Aurora DSQL
2. Refer the [demo application](demo-app/README.md) to build and test REST APIs interacting with your Amazon Aurora DSQL using `curl` commands

## Clean up

1. Shutdown the locally running Spring Boot application
2. Delete the [CDK infrastructure stack](infrastructure/README.md)
3. Delete the Amazon Aurora DSQL cluster

# Security

See [CONTRIBUTING](./CONTRIBUTING.md#security-issue-notifications) for more information.

# License

This library is licensed under the MIT-0 License. See the [LICENSE](./LICENSE) file.
