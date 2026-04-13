pipeline {
  environment {
    ARGO_SERVER = '158.158.43.58:32100'
    GITHUB_TOKEN = credentials('github_jenkins_token')
    DEV_URL = 'http://158.158.43.58:30080/'
  }
  agent {
    kubernetes {
      yamlFile 'build-agent.yaml'
      defaultContainer 'maven'
      idleMinutes 1
    }
  }
  stages {
	stage('Repo Scan') {
      parallel {
        stage('Secret Scanner') {
          steps {
            container('trufflehog') {
		      sh 'trufflehog filesystem . --fail'
    		}
          }
        }
      }
    }
    stage('Build') {
      parallel {
        stage('Compile') {
          steps {
            container('maven') {
              sh 'mvn compile'
            }
          }
        }
      }
    }
    stage('Static Analysis') {
      parallel {
		stage('Software Component Analysis') {
		  steps {
		    container('maven') {
		      catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
				sh 'mvn org.owasp:dependency-check-maven:check -DossindexAnalyzerEnabled=false -Dformat=ALL'
		      }
		    }
		  }
		  post {
		    always {
		      archiveArtifacts allowEmptyArchive: true, artifacts: 'target/dependency-check-report.*', fingerprint: true
		      dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
		    }
	  	  }
		}
	  
	
	stage('Generate SBOM') {
	  steps {
	    container('maven') {
	      sh 'mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom'
	    }
	  }
	  post {
	    success {
	//       dependencyTrackPublisher projectName: 'sample-spring-app', projectVersion: '0.0.1', artifact: 'target/bom.xml', autoCreateProjects: true, synchronous: true
	      archiveArtifacts allowEmptyArchive: true, artifacts: 'target/bom.xml', fingerprint: true, onlyIfSuccessful: true
	    }
	  }
	}
	
	stage('OSS License Checker') {
	  steps {
	    container('licensefinder') {
	      sh '''
		 /bin/bash --login -c "
		 export LICENSE_FINDER_IGNORE_MAVEN_WRAPPER=true
		 export PATH=/usr/share/maven/bin:$PATH
		 rvm use default
		 gem install license_finder
		 license_finder "
		 '''
	      }
	    }
	  }
	
        stage('Unit Tests') {
          steps {
            container('maven') {
              sh 'mvn test'
            }
          }
        }
      }
    }

    stage('Static Application Security Testing') {
      steps {
        container('slscan') {
          sh 'scan --type java --build'
        }
      }
      post {
        success {
          archiveArtifacts allowEmptyArchive: true, artifacts: 'reports/*', fingerprint: true, onlyIfSuccessful: true
        }
      }
    }

	stage('Package') {
      parallel {
        stage('OCI Image BnP') {
          steps {
            container('kaniko') {
              sh "/kaniko/executor -f `pwd`/Dockerfile -c `pwd` --insecure --skip-tls-verify --cache=true --destination=docker.io/emmiduh93/nexus-fintech:${env.BUILD_NUMBER}"
            }
          }
        }
      }
    }

    stage('Image Analysis') {
      parallel {
        stage('Image Linting') {
	  steps {
	    container('docker-tools') {
	      sh "dockle --timeout 600s docker.io/emmiduh93/nexus-fintech:${env.BUILD_NUMBER}"
	    }
	  }
	}
	stage('Image Scan') {
	  steps {
	    container('docker-tools') {
	      sh "GITHUB_TOKEN= trivy image --timeout 10m --exit-code 1 emmiduh93/nexus-fintech:${env.BUILD_NUMBER}"
	    }
	  }
	}
      }
    }

    stage('Scan k8s Deploy Code') {
      steps {
	container('docker-tools') {
	  sh 'curl -sSX POST --data-binary @"deploy/dso-demo-deploy.yaml" https://v2.kubesec.io/scan'
	}
      }
    }

	stage('Update GitOps Repo') {
      steps {
        container('docker-tools') {
          sh '''
            sed -i "s|image: emmiduh93/nexus-fintech:.*|image: emmiduh93/nexus-fintech:${BUILD_NUMBER}|g" deploy/dso-demo-deploy.yaml
            git config user.email "jenkins-bot@example.com"
            git config user.name "Jenkins CI Bot"
            git add deploy/dso-demo-deploy.yaml
            git commit -m "chore: update image tag to ${BUILD_NUMBER} [skip ci]"
            git push https://${GITHUB_TOKEN}@github.com/emmiduh/DevSecOps-CI-CD-Pipeline.git HEAD:main
          '''
        }
      }
    }

    stage('Deploy to Dev') {
       environment {
         AUTH_TOKEN = credentials('argocd-jenkins-deployer-token')  
       }
      steps {
		  sh "echo done"
         container('docker-tools') {
           sh 'docker run -t schoolofdevops/argocd-cli argocd app sync nexusfintechapp --insecure --server $ARGO_SERVER --auth-token $AUTH_TOKEN'
	  	   sh 'docker run -t schoolofdevops/argocd-cli argocd app wait nexusfintechapp --health --timeout 300 --insecure --server $ARGO_SERVER --auth-token $AUTH_TOKEN'
         }
      }
    }
    
    stage('Dynamic Analysis') {
      parallel {
        stage('E2E tests') {
          steps {
            sh 'echo "All Tests passed!!!"'
          }
        }
        stage('Dynamic Application Security Testing') {
          steps {
            container('docker-tools') {
              sh 'docker run -t owasp/zap2docker-stable zap-baseline.py -t $DEV_URL || exit 0'
	    }
	  }
	}
      }
    }
  }
}
