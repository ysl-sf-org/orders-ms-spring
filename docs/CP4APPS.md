###### orders-ms-spring

# Running Orders App on CP4Apps

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://github.com/ibm-garage-ref-storefront/refarch-cloudnative-storefront*

## Table of Contents

* [Introduction](#introduction)
    + [APIs](#apis)
* [Pre-requisites](#pre-requisites)
* [Orders application on CP4Apps](#orders-application-on-cp4apps)
    + [Get the Orders application](#get-the-orders-application)
    + [Application manifest](#application-manifest)
    + [Project Setup](#project-setup)
    + [Deploy MariaDB to Openshift](#deploy-mariadb-to-openshift)
    + [Deploy the app using Kabanero Pipelines](#deploy-the-app-using-kabanero-pipelines)
      * [Access tekton dashboard](#access-tekton-dashboard)
      * [Create registry secrets](#create-registry-secrets)
      * [Create Webhook for the app repo](#create-webhook-for-the-app-repo)
      * [Deploy the app](#deploy-the-app)
* [Conclusion](#conclusion)

## Introduction

This project will demonstrate how to deploy a Spring Boot Application with a MariaDB database onto a Kubernetes Cluster.

![Application Architecture](static/orders.png?raw=true)

Here is an overview of the project's features:
- Leverages [`Spring Boot`](https://projects.spring.io/spring-boot/) framework to build a Microservices application.
- Uses [`MariaDB`](https://mariadb.org/) as the orders database.
- Uses [`Spring Data JPA`](http://projects.spring.io/spring-data-jpa/) to persist data to MariaDB database.
- [OAuth 2.0](https://oauth.net/2/) protected APIs using Spring Security framework.
- Uses [`Docker`](https://docs.docker.com/) to package application binary and its dependencies.
- When retrieving orders using the OAuth 2.0 protected APIs, return only orders belonging to the user identity encoded in the user_name claim in the JWT payload. For more details on how identity is propagated, refer [Auth](https://github.com/ibm-garage-ref-storefront/auth-ms-spring/blob/master/docs/CP4APPS.md) Microservice.

### APIs

The Orders Microservice REST API is OAuth 2.0 protected. These APIs identifies and validates the caller using signed JWT tokens.

* `GET /micro/orders`

  Returns all orders. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim. A JSON object array is returned consisting of only orders created by the orders ID.

* `GET /micro/orders/{id}`

  Return order by ID. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim. If the id of the order is owned by the orders passed in the header, it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

* `POST /micro/orders`

  Create an order. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim. The Order object must be passed as JSON object in the request body with the following format.

  ```
    {
      "itemId": "item_id",
      "count": "number_of_items_in_order",
    }
  ```

  On success, `HTTP 201` is returned with the ID of the created order in the `Location` response header.

## Pre-requisites:

* [RedHat Openshift Cluster](https://cloud.ibm.com/kubernetes/catalog/openshiftcluster)

* IBM Cloud Pak for Applications
  + [Using IBM Console](https://cloud.ibm.com/catalog/content/ibm-cp-applications)
  + [OCP4 CLI installer](https://www.ibm.com/support/knowledgecenter/en/SSCSJL_4.1.x/install-icpa-cli.html)

* Docker Desktop
  + [Docker for Mac](https://docs.docker.com/docker-for-mac/)
  + [Docker for Windows](https://docs.docker.com/docker-for-windows/)

* Command line (CLI) tools
  + [oc](https://www.okd.io/download.html)
  + [git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
  + [appsody](https://appsody.dev/docs/getting-started/installation)

* Deploy Auth Microservice - For instructions, refer [here](https://github.com/ibm-garage-ref-storefront/auth-ms-spring).

## Orders application on CP4Apps

### Get the Orders application

- Clone orders repository:

```bash
git clone https://github.com/ibm-garage-ref-storefront/orders-ms-spring.git
cd orders-ms-spring
```

### Application manifest

When you see the project structure, you should be able to find an `app-deploy.yaml`. This is generated as follows.

```
appsody deploy --generate-only
```

This generates a default `app-deploy.yaml` and on top of this we added necessary configurations that are required by the Orders application.

### Project Setup

- Create a new project if it does not exist. Or if you have an existing project, skip this step.

```
oc new-project storefront
```

- Once the namespace is created, we need to add it as a target namespace to Kabanero.

Verify if kabanero is present as follows.

```
$ oc get kabaneros -n kabanero
NAME       AGE   VERSION   READY
kabanero   9d    0.6.1     True
```

- Edit the yaml file configuring kabanero as follows.

```
$ oc edit kabanero kabanero -n kabanero
```

- Finally, navigate to the spec label within the file and add the following targetNamespaces label.

```
spec:
  targetNamespaces:
    - storefront
```

### Deploy MariaDB to Openshift

- Now deploy MariaDB as follows.

```
oc apply --recursive --filename MariaDB/
```

- Verify if the pods are up and running.

```
$ oc get pods
NAME                                   READY   STATUS    RESTARTS   AGE
orders-mariadb-0                       1/1     Running   0          130m
```

### Deploy the app using Kabanero Pipelines

#### Access tekton dashboard

- Open IBM Cloud Pak for Applications and click on `Instance` section. Then select `Manage Pipelines`.

![CP4Apps](static/cp4apps_pipeline.png?raw=true)

- This will open up the Tekton dashboard.

![Tekton dashboard](static/tekton.png?raw=true)

#### Create registry secrets

- To create a secret, in the menu select `Secrets` > `Create` as below.

![Secret](static/secret.png?raw=true)

Provide the below information.

```
Name - <Name for secret>
Namespace - <Your pipeline namespace>
Access To - Docker registry>
username - <registry user name>
Password/Token - <registry password or token>
Service account - kabanero-pipeline
Server Url - Keep the default one
```

- You will see a secret like this once created.

![Docker registry secret](static/docker_registry_secret.png?raw=true)

#### Create Webhook for the app repo

- For the Github repo, create the webhook as follows. To create a webhook, in the menu select `Webhooks` > `Create webhook`

We will have below

![Webhook](static/webhook.png?raw=true)

Provide the below information.

```
Name - <Name for webhook>
Repository URL - <Your github repository URL>
Access Token - <For this, you need to create a Github access token with permission `admin:repo_hook` or select one from the list>
```

To know more about how to create a personal access token, refer [this](https://help.github.com/en/articles/creating-a-personal-access-token-for-the-command-line).

- Now, enter the pipeline details.

![Pipeline Info](static/pipeline_info.png?raw=true)

- Once you click create, the webhook will be generated.

![Orders Webhook](static/webhook_orders.png?raw=true)

- You can also see in the app repo as follows.

![Orders Repo Webhook](static/webhook_orders_repo.png?raw=true)

#### Deploy the app

Whenever we make changes to the repo, a pipeline run will be triggered and the app will be deployed to the openshift cluster.

- To verify if it is deployed, run below command.

```
oc get pods
```

If it is successful, you will see something like below.

```
$ oc get pods
NAME                                   READY   STATUS    RESTARTS   AGE
orders-mariadb-0                       1/1     Running   0          130m
orders-ms-spring-567f57dc5f-8b5q6      1/1     Running   0          83m
```

- You can access the app as below.

```
oc get route
```

This will return you something like below.

```
$ oc get route
NAME                  HOST/PORT                                                                                                                      PATH   SERVICES              PORT       TERMINATION   WILDCARD
orders-ms-spring      orders-ms-spring-storefront.csantana-demos-ocp43-fa9ee67c9ab6a7791435450358e564cc-0000.us-east.containers.appdomain.cloud             orders-ms-spring      8080-tcp                 None
```

- Grab the `jwt` token from the [Auth](https://github.com/ibm-garage-ref-storefront/auth-ms-spring/blob/master/docs/CP4APPS.md) Microservice.

- Now, you can validate the application as follows.

* Create an Order

Run the following to create an order for the `admin` user. Be sure to use the JWT retrieved from the previous step in place of `${jwt}`.

```
curl -i -H "Content-Type: application/json" -H "Authorization: Bearer ${jwt}" -X POST -d '{"itemId":13401, "count":1}' "http://<orders_host>:<orders_port>/micro/orders"
```

If successfully created, you will see something like below.

```
$ curl -i -H "Content-Type: application/json" -H "Authorization: Bearer ${jwt}" -X POST -d '{"itemId":13401, "count":1}' "http://orders-ms-spring-storefront.csantana-demos-ocp43-fa9ee67c9ab6a7791435450358e564cc-0000.us-east.containers.appdomain.cloud/micro/orders"
HTTP/1.1 201
Location: http://orders-ms-spring-storefront.csantana-demos-ocp43-fa9ee67c9ab6a7791435450358e564cc-0000.us-east.containers.appdomain.cloud/micro/orders/2c91808371ee5aa50171ee653f440000
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
Date: Thu, 07 May 2020 09:09:05 GMT
```

* Get all Orders

Run the following to retrieve all orders for the `admin` customerId. Be sure to use the JWT retrieved from the previous step in place of `${jwt}`.

```
curl -H "Authorization: Bearer ${jwt}" "http://<orders_host>:<orders_port>/micro/orders"
```

If it is running successfully, you will see something like below.

```
$ curl -H "Authorization: Bearer ${jwt}" "http:http://orders-ms-spring-storefront.csantana-demos-ocp43-fa9ee67c9ab6a7791435450358e564cc-0000.us-east.containers.appdomain.cloud/micro/orders"
[{"id":"2c91808371ee5aa50171ee653f440000","date":"2020-05-07T09:09:04.000+0000","itemId":13401,"customerId":"admin","count":1}]
```

## Conclusion

You have successfully deployed and tested the Orders Microservice and a MariaDB database on Openshift using IBM Cloud Paks for Apps.

To see the Orders application working in a more complex microservices use case, checkout our Microservice Reference Architecture Application [here](https://github.com/ibm-garage-ref-storefront/refarch-cloudnative-storefront).
