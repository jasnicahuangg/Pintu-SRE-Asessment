# Pintu-SRE-Asessment
If u run in docker just build the image from Dockerfile and run the container using the image. For run App in local, you must follow this below:
## 🎬 Getting started

- Create a virtual environment using your favorite tool:

```bash
$ virtualenv -p python venv
```

- Install the project in [`editable`](https://setuptools.pypa.io/en/latest/userguide/development_mode.html) mode:

```bash
$ python -m pip install -e .
```

- Run:

```bash
$ python src/python_project_boilerplate/main.py
```

or using the console script defined in the [setup.cfg](./setup.cfg) file:

```bash
$ python_project_boilerplate
```

## Solution
1. For the simple app i used the boiler plate copied from : https://github.com/compiuta-origin/python-project-boilerplate/tree/development
2. For CI/CD Pipelines i used Jenkins to automate building and testing. I seperate jobs for CI & CD.
```
CI :
- Do building image using Dockerfile and push the image to AWS ECR (assume ECR Repo already created)
- Do testing using pytest. Execute pytest in docker container

CD :
- Have to choose environment staging or prod which need deploy. Assume I run my kubernetes in EKS so the deployment have staging & prod cluster
- Each of them have own kube config, so the deployment will executed based on env. In the deployment process will monitor status deployment is available or not.
- If available means the deployment succeed, other than that means the deployment have issue and need to recover ASAP. To recover that used rollback deployment to previous version
```

4. For manifest kubernetes have 3 files: deployment.yaml, service.yaml & ingress.yaml
   Especially for service, the domain name used nip.io with ip LB (assumed ingress created in EKS and generate ip public)


