#!/usr/bin/env node

// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import * as cdk from 'aws-cdk-lib';
import {AuroraDsqlSecretCreationAndRotationStack} from '../lib/aurora-dsql-secret-rotation-stack';

const app = new cdk.App();

new AuroraDsqlSecretCreationAndRotationStack(app, 'AuroraDsqlSecretRotationStack', {
    secretName: '/aws/aurora-dsql/secret',
    dsqlClusterEndpoint: `${process.env.DSQL_CLUSTER_ID}.dsql.${process.env.CDK_DEFAULT_REGION}.on.aws`,
    dsqlClusterArn: `arn:aws:dsql:${process.env.CDK_DEFAULT_REGION}:${process.env.CDK_DEFAULT_ACCOUNT}:cluster/${process.env.DSQL_CLUSTER_ID}`,
    logGroupName: '/aws/aurora-dsql/secret-rotation',
    description: 'Secret rotation for Aurora DSQL',
    env: {
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: process.env.CDK_DEFAULT_REGION
    }
});