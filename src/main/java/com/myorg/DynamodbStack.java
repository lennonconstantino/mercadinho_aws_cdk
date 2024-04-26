package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.constructs.Construct;

public class DynamodbStack extends Stack {
    private final Table productEventsDynamodb;

    public DynamodbStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamodbStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        productEventsDynamodb = createProvisioned();
    }

    public Table getProductEventsDynamodb() {
        return productEventsDynamodb;
    }

    private Table createOnDemand() {
        return Table.Builder.create(this, "ProductEventsDb")
                .tableName("product-events")
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private Table createProvisioned() {
        return Table.Builder.create(this, "ProductEventsDb")
                .tableName("product-events")
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(1)
                .writeCapacity(1)
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private Table createProvisionedWithAutoScalling() {
        Table table = Table.Builder.create(this, "ProductEventsDb")
                .tableName("product-events")
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(1)
                .writeCapacity(1)
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Auto Scaling
        productEventsDynamodb.autoScaleReadCapacity(
                        EnableScalingProps.builder()
                                .minCapacity(1)
                                .maxCapacity(4)
                                .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());

        productEventsDynamodb.autoScaleWriteCapacity(
                        EnableScalingProps.builder()
                                .minCapacity(1)
                                .maxCapacity(4)
                                .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());

        return table;
    }
}
