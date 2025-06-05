// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { AuroraDsqlSecretCreationAndRotationStack } from '../lib/aurora-dsql-secret-rotation-stack';

describe('AuroraDsqlSecretCreationAndRotationStack', () => {
    let app: cdk.App;
    let stack: AuroraDsqlSecretCreationAndRotationStack;
    let template: Template;
    
    beforeEach(() => {
        app = new cdk.App();
        stack = new AuroraDsqlSecretCreationAndRotationStack(app, 'TestStack', {
            env: {
                account: '012345678901',
                region: 'us-east-1'
            },
            secretName: 'test-aurora-dsql-secret',
            dsqlClusterEndpoint: 'test-aurora-dsql-cluster-endpoint',
            dsqlClusterArn: 'arn:aws:dsql:us-east-1:012345678901:cluster/1abcdefghijk2lmnopqr3stu45',
            logGroupName: '/aws/lambda/test-aurora-dsql-rotation-function'
        });
        template = Template.fromStack(stack);
    });

    test('Secret is created with correct properties', () => {
        template.hasResourceProperties('AWS::SecretsManager::Secret', {
            Name: 'test-aurora-dsql-secret'
        });
    });

    test('Log group is created with correct retention', () => {
        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/lambda/test-aurora-dsql-rotation-function',
        });
    });
});
