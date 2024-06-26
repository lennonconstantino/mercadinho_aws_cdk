package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic, Bucket invoiceBucket, Queue invoiceQueue) {
        this(scope, id, null, cluster, productEventsTopic, invoiceBucket, invoiceQueue);
    }

    public Service01Stack(final Construct scope,
                          final String id,
                          final StackProps props,
                          Cluster cluster,
                          SnsTopic productEventsTopic,
                          Bucket invoiceBucket,
                          Queue invoiceQueue
    ) {
        super(scope, id, props);

        // 06
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("SPRING_DATASOURCE_URL", "jdbc:mariadb://" + Fn.importValue("rds-endpoint")
                + ":3306/aws_project01?createDatabaseIfNotExist=true");
        envVariables.put("SPRING_DATASOURCE_USERNAME", "admin");
        envVariables.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("rds-password"));
        //09
        envVariables.put("AWS_REGION", "us-east-1");
        envVariables.put("AWS_SNS_TOPIC_PRODUCT_EVENTS_ARN", productEventsTopic.getTopic().getTopicArn());

        envVariables.put("AWS_S3_BUCKET_INVOICE_NAME", invoiceBucket.getBucketName());
        envVariables.put("AWS_SQS_QUE_INVOICE_EVENTS_NAME", invoiceQueue.getQueueName());

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB01")
                .serviceName("service-01")
                .cluster(cluster)
                .cpu(512)
                .desiredCount(2)
                .listenerPort(8080)
                .memoryLimitMiB(1024)
                .assignPublicIp(true)  // 02 - opcao para quem optar em não usar o NAT-Gtw
                .taskImageOptions(
                        // 01 - aqui definimos qual vai ser a imagem que vai ser utilizada dentro do nosso serviço
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("aws_project01")
                                .image(ContainerImage.fromRegistry("lennomolliver/mercadinho:0.0.7-SNAPSHOT"))
                                .containerPort(8080)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "Service01LogGroup")
                                                .logGroupName("Service01")
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix("Service01")
                                        .build()))
                                .environment(envVariables) // 06
                                .build())
                .publicLoadBalancer(true)
                .build();

        // 03 - configurando o Target Group - monitorar a aplicação
        service01.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .path("/actuator/health") // o caminho para saber se o health check esta funcionando
                .port("8080")
                .healthyHttpCodes("200")
                .build());

        // 04 - configurando o AutoScale
        // 04.1 - parametros que o autoscale vai atuar
        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4) // aumentar demais pode interferir nas conexoes de banco de dados
                .build());

        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60)) // periodo de analise para destruir a instancia
                .build());

        // 08 dando permissão para o ECS publicar mensagens no topico criado
        productEventsTopic.getTopic().grantPublish(service01.getTaskDefinition().getTaskRole());

        // permissão para consumir mensagens da fila
        invoiceQueue.grantConsumeMessages(service01.getTaskDefinition().getTaskRole());
        invoiceBucket.grantReadWrite(service01.getTaskDefinition().getTaskRole());

    }
}