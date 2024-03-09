pipeline {
    agent any

    triggers {
        // Trigger the pipeline on every push to the repository
        scm '*/5 * * * *'
    }

    environment {
        AWS_DEFAULT_REGION = 'us-east-1'
        AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
        ECR_REGISTRY = '563940537117.dkr.ecr.us-east-1.amazonaws.com'
        DOCKER_IMAGE_NAME = 'python-project-boilerplate'
    }

    stages {
        stage('Clone Repository') {
            steps {
                git 'https://github.com/jasnicahuangg/Pintu-SRE-Asessment.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh 'docker build -t ${DOCKER_IMAGE_NAME}:${env.BUILD_ID} .'
                }
            }
        }

        stage('Push Docker Image to AWS ECR') {
            steps {
                script {
                    // Authenticate with AWS ECR
                    withCredentials([string(credentialsId: 'aws-ecr-credentials', variable: 'AWS_CREDENTIALS')]) {

                        sh "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
                    }

                    // Tag Docker image for ECR
                    sh "docker tag ${DOCKER_IMAGE_NAME}:${env.BUILD_ID} ${ECR_REGISTRY}/${DOCKER_IMAGE_NAME}:${env.BUILD_ID}"
                    sh "docker tag ${DOCKER_IMAGE_NAME}:${env.BUILD_ID} ${ECR_REGISTRY}/${DOCKER_IMAGE_NAME}:latest"

                    // Push Docker image to AWS ECR
                    sh "docker push ${ECR_REGISTRY}/${DOCKER_IMAGE_NAME}:${env.BUILD_ID}"
                    sh "docker rmi ${DOCKER_IMAGE_NAME}:${env.BUILD_ID}"
                    sh "docker rmi ${DOCKER_IMAGE_NAME}:latest"
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    // Pull Docker image from AWS ECR
                    sh "docker pull ${ECR_REGISTRY}/${DOCKER_IMAGE_NAME}:${env.BUILD_ID}"

                    // Run pytest tests inside the Docker container
                    sh "docker run --rm ${ECR_REGISTRY}/${DOCKER_IMAGE_NAME}:${env.BUILD_ID} pytest"
                }
            }
        }
    }
}

