// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { Construct } from 'constructs';
import * as path from 'path';

interface AuroraDsqlSecretCreationAndRotationStackProps extends cdk.StackProps {
    secretName: string;
    logGroupName: string;
    dsqlClusterEndpoint: string;
    dsqlClusterArn: string;
    applicationRoleArn?: string;
}

export class AuroraDsqlSecretCreationAndRotationStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props: AuroraDsqlSecretCreationAndRotationStackProps) {
        super(scope, id, props);

        // Create a Lambda Layer
        const pythonLayer = new lambda.LayerVersion(this, 'AuroraDSQLSecretRotationLayer', {
            layerVersionName: 'aurora-dsql-secret-rotation',
            code: lambda.Code.fromAsset(path.join(__dirname, 'functions/secret-rotation/layer')),
            compatibleRuntimes: [
                lambda.Runtime.PYTHON_3_13
            ],
            compatibleArchitectures: [
                lambda.Architecture.X86_64
            ],
            description: 'Python dependencies for Aurora DSQL secret rotation',
        });

        // Create the CloudWatch Logs group
        const logGroup = new logs.LogGroup(this, 'AuroraDSQLSecretRotationLambdaLogGroup', {
            logGroupName: props.logGroupName,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });

        // Create IAM Role for Lambda
        const lambdaRole = new iam.Role(this, 'AuroraDSQLSecretRotationLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            description: 'Role for Aurora DSQL secret rotation Lambda function'
        });

        // Create the IAM managed policy for secret rotation Lambda function
        new iam.ManagedPolicy(this, 'AuroraDSQLSecretRotationLambdaPolicy', {
            description: 'Policy for Aurora DSQL secret rotation Lambda function',
            roles: [lambdaRole],
            statements: [
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'secretsmanager:DescribeSecret',
                        'secretsmanager:GetSecretValue',
                        'secretsmanager:PutSecretValue',
                        'secretsmanager:UpdateSecretVersionStage'
                    ],
                    resources: [
                        `arn:aws:secretsmanager:${cdk.Stack.of(this).region}:${cdk.Stack.of(this).account}:secret:${props.secretName}`
                    ]
                }),
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'logs:CreateLogStream',
                        'logs:CreateLogGroup',
                        'logs:PutLogEvents'
                    ],
                    resources: [
                        `arn:aws:logs:${cdk.Stack.of(this).region}:${cdk.Stack.of(this).account}:log-group:${props.logGroupName}:*`
                    ]
                }),
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'dsql:GetCluster',
                        'dsql:ListClusters',
                        'dsql:DbConnect',
                        'dsql:DbConnectAdmin'
                    ],
                    resources: [props.dsqlClusterArn]
                })
            ]
        });

        // Create Lambda Function
        const pythonFunction = new lambda.Function(this, 'AuroraDSQLSecretRotationFunction', {
            runtime: lambda.Runtime.PYTHON_3_13,
            architecture: lambda.Architecture.X86_64,
            handler: 'lambda_function.lambda_handler',
            code: lambda.Code.fromAsset(path.join(__dirname, 'functions/secret-rotation/src')),
            layers: [pythonLayer],
            role: lambdaRole,
            logGroup: logGroup,
            timeout: cdk.Duration.seconds(60),
            memorySize: 128,
            environment: {
                LOG_LEVEL: 'INFO'
            },
            description: 'Lambda function for Aurora DSQL secret rotation',
            functionName: 'aurora-dsql-secret-rotation'
        });

        // Add resource-based policy to allow Secrets Manager to invoke the function
        pythonFunction.addPermission('SecretsManagerInvoke', {
            principal: new iam.ServicePrincipal('secretsmanager.amazonaws.com'),
            action: 'lambda:InvokeFunction',
            sourceAccount: cdk.Stack.of(this).account,
            sourceArn: `arn:aws:secretsmanager:${cdk.Stack.of(this).region}:${cdk.Stack.of(this).account}:secret:${props.secretName}`
        });

        // Create the secret for Aurora DSQL cluster
        const secret = new secretsmanager.Secret(this, 'AuroraDSQLSecret', {
            secretName: props.secretName,
            description: 'Secret for Aurora DSQL cluster',
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            secretObjectValue: {
                username: cdk.SecretValue.unsafePlainText('admin'),
                password: cdk.SecretValue.unsafePlainText('TO-BE-ROTATED'),
                host: cdk.SecretValue.unsafePlainText(props.dsqlClusterEndpoint),
                engine: cdk.SecretValue.unsafePlainText('postgres'),
                port: cdk.SecretValue.unsafePlainText('5432'),
                dbname: cdk.SecretValue.unsafePlainText('postgres')
            }
        });

        // Create a resource policy for the secret to restrict access
        // These policies allow only the Lambda rotation function and optionally the application role to access the secret

        secret.addToResourcePolicy(new iam.PolicyStatement({
            effect: iam.Effect.DENY,
            principals: [
                new iam.AccountRootPrincipal()
            ],
            actions: [
                'secretsmanager:DeleteSecret'
            ],
            resources: ['*']
        }));

        secret.addToResourcePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            principals: [
                new iam.ServicePrincipal('secretsmanager.amazonaws.com')
            ],
            actions: [
                'secretsmanager:*'
            ],
            resources: ['*']
        }));

        secret.addToResourcePolicy(new iam.PolicyStatement({
            effect: iam.Effect.DENY,
            principals: [
                new iam.AnyPrincipal()
            ],
            actions: [
                'secretsmanager:DescribeSecret',
                'secretsmanager:GetSecretValue',
                'secretsmanager:PutSecretValue',
                'secretsmanager:UpdateSecretVersionStage'
            ],
            resources: ['*'],
            conditions: {
                'StringNotEquals': {
                    'aws:PrincipalArn': [
                        lambdaRole.roleArn,
                        // Include application role if provided
                        ...(props.applicationRoleArn ? [props.applicationRoleArn] : [])
                    ]
                },
                'ArnNotLike': {
                    'aws:PrincipalArn': 'arn:aws:iam::*:role/aws-service-role/secretsmanager.amazonaws.com/*'
                }
            }
        }));

        // Add rotation schedule
        secret.addRotationSchedule('AuroraDSQLSecretRotationSchedule', {
            rotationLambda: pythonFunction,
            automaticallyAfter: cdk.Duration.hours(4),
            rotateImmediatelyOnUpdate: true
        });
    }
}
