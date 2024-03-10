pipeline {
    agent any

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['staging', 'prod'], description: 'Choose deployment environment: ')
    }

    environment {
        AWS_DEFAULT_REGION = 'us-east-1'
        AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
        ECR_REGISTRY = '563940537117.dkr.ecr.us-east-1.amazonaws.com'
        DOCKER_IMAGE_NAME = 'python-project-boilerplate'
        DOCKER_IMAGE_TAG = 'latest'
        KUBE_CONFIG = credentials("kube-config-${ENVIRONMENT}")
        DEPLOYMENT_NAME = 'python-boilerplate'
        NAMESPACE = 'pintu-assessment'
    }

    stages {
        stage('Clone Repository') {
            steps {
                git 'https://github.com/jasnicahuangg/Pintu-SRE-Asessment.git'
            }
        }

        stage('Deploy to EKS') {
            steps {
                script {
                    // Authenticate with AWS ECR
                    withCredentials([string(credentialsId: 'aws-ecr-credentials-id', variable: 'AWS_CREDENTIALS')]) {
                        sh "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
                    }

                    // Pull Docker image from AWS ECR
                    sh "docker pull ${ECR_REGISTRY}/${DOCKER_IMAGE_NAME}:${env.BUILD_ID}"

                    // Deploy to ELK using Kubernetes manifests
                    sh "kubectl --kubeconfig=${KUBE_CONFIG} apply -f ./kubernetes/deployment.yaml"

                    // Wait for deployment to finish
                    timeout(time: 5, unit: 'minutes') {
                        waitUntil {
                            def status = sh(script: "kubectl --kubeconfig=${KUBE_CONFIG} get deployment ${DEPLOYMENT_NAME} -n ${NAMESPACE} -o jsonpath='{.status.conditions[?(@.type==\"Available\")].status}'", returnStatus: true).trim()
                            return status == 'True'
                        }
                    }
                }
            }
        }

        stage('Rollback') {
            when {
                expression {
                    // Check deployment status
                    def status = sh(script: "kubectl --kubeconfig=${KUBE_CONFIG} get deployment ${DEPLOYMENT_NAME} -n ${NAMESPACE} -o jsonpath='{.status.conditions[?(@.type==\"Available\")].status}'", returnStatus: true).trim()
                    return status != 'True'
                }
            }
            steps {
                script {
                    // Rollback deployment
                    sh "kubectl --kubeconfig=${KUBE_CONFIG} rollout undo deployment ${DEPLOYMENT_NAME} -n ${NAMESPACE}"
                }
            }
        }
    }
}

