package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class SnsStack extends Stack {
    private final SnsTopic productEventsTopic;

    public SnsStack(final Construct scope, final String id, SnsTopic productEventsTopic) {
        this(scope, id, null, productEventsTopic);
    }

    public SnsStack(final Construct scope, final String id, final StackProps props, SnsTopic productEventsTopic) {
        super(scope, id, props);
        this.productEventsTopic = productEventsTopic;

        productEventsTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "ProductEventsTopic")
                .topicName("product-events")
                .build()).build();

        productEventsTopic.getTopic().addSubscription(EmailSubscription.Builder.create("lennonconstantino@gmail.com")
                .json(true)
                .build());
    }

    public SnsTopic getProductEventsTopic() {
        return productEventsTopic;
    }
}