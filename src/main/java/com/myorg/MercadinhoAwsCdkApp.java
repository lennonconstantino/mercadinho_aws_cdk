package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.Arrays;

public class MercadinhoAwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        VpcStack vpcStack = new VpcStack(app, "Vpc");

        // a stack do cluster depende do vpc
        ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        // 06
        RdsStack rdsStack = new RdsStack(app, "Rds", vpcStack.getVpc());
        rdsStack.addDependency(vpcStack);

        // 07
        SnsStack snsStack = new SnsStack(app, "Sns");

        // 05.01 Deployment dos recursos
        // a stack de servico depende do cluster e do vpc
        Service01Stack service01Stack = new Service01Stack(app, "Service01", clusterStack.getCluster(), snsStack.getProductEventsTopic());
        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);

        // 11 criando o dynamodb
        DynamodbStack dynamodbStack = new DynamodbStack(app, "Dynamodb");

        // 10 criando o serviço 2
        Service02Stack service02Stack = new Service02Stack(app
                , "Service02"
                , clusterStack.getCluster()
                , snsStack.getProductEventsTopic()
                , dynamodbStack.getProductEventsDynamodb()
        );
        service02Stack.addDependency(snsStack);
        service02Stack.addDependency(clusterStack);
        service02Stack.addDependency(dynamodbStack);

        app.synth();
    }
}

