##### orders-ms-spring

# Microservice Apps Integration with MariaDB Database and enabling OAuth protection for APIs

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://cloudnativereference.dev/*

## Table of Contents

* [Introduction](#introduction)
    + [APIs](#apis)
* [Pre-requisites](#pre-requisites)
* [Implementation Details](#implementation-details)
* [Running the application on Docker](#running-the-application-on-docker)
    + [Get the Orders application](#get-the-inventory-application)
    + [Run the MariaDB Docker Container](#run-the-mysql-docker-container)
    + [Run the Orders application](#run-the-inventory-application)
    + [Validating the application](#validating-the-application)
    + [Exiting the application](#exiting-the-application)
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
- When retrieving orders using the OAuth 2.0 protected APIs, return only orders belonging to the user identity encoded in the user_name claim in the JWT payload. For more details on how identity is propagated, refer [Auth](https://github.com/ibm-garage-ref-storefront/auth-ms-spring) Microservice.

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

* [Appsody](https://appsody.dev/)
    + [Installing on MacOS](https://appsody.dev/docs/installing/macos)
    + [Installing on Windows](https://appsody.dev/docs/installing/windows)
    + [Installing on RHEL](https://appsody.dev/docs/installing/rhel)
    + [Installing on Ubuntu](https://appsody.dev/docs/installing/ubuntu)
For more details on installation, check [this](https://appsody.dev/docs/installing/installing-appsody/) out.

* Docker Desktop
    + [Docker for Mac](https://docs.docker.com/docker-for-mac/)
    + [Docker for Windows](https://docs.docker.com/docker-for-windows/)

## Implementation Details

We created a new spring boot project using appsody as follows.

```
appsody repo add kabanero https://github.com/kabanero-io/kabanero-stack-hub/releases/download/0.6.5/kabanero-stack-hub-index.yaml

appsody init kabanero/java-spring-boot2
```

And then we defined the necessary code for the application on top on this template.

## Running the application on Docker

### Get the Orders application

- Clone orders repository:

```bash
git clone https://github.com/ibm-garage-ref-storefront/orders-ms-spring.git
cd orders-ms-spring
```

### Run the MariaDB Docker Container

Run the below command to get MariaDB running via a Docker container.

```bash
# Start a MariaDB Container with a database user, a password, and create a new database
docker run --name ordersmysql \
    -e MYSQL_ROOT_PASSWORD=admin123 \
    -e MYSQL_USER=dbuser \
    -e MYSQL_PASSWORD=password \
    -e MYSQL_DATABASE=ordersdb \
    -p 3306:3306 \
    -d mariadb
```

If it is successfully deployed, you will see something like below.

```
$ docker ps
CONTAINER ID        IMAGE                            COMMAND                  CREATED             STATUS              PORTS                                                                                              NAMES
ae5ed47cb0be        mariadb                          "docker-entrypoint.s…"   45 minutes ago      Up 45 minutes       0.0.0.0:3306->3306/tcp                                                                             ordersmysql
```

### Run the Orders application

- Before running the application, make sure you grab the `HS256` shared secret.

To make things easier for you, we pasted below the 2048-bit secret here.

```
E6526VJkKYhyTFRFMC0pTECpHcZ7TGcq8pKsVVgz9KtESVpheEO284qKzfzg8HpWNBPeHOxNGlyudUHi6i8tFQJXC8PiI48RUpMh23vPDLGD35pCM0417gf58z5xlmRNii56fwRCmIhhV7hDsm3KO2jRv4EBVz7HrYbzFeqI45CaStkMYNipzSm2duuer7zRdMjEKIdqsby0JfpQpykHmC5L6hxkX0BT7XWqztTr6xHCwqst26O0g8r7bXSYjp4a
```

As the APIs in this microservice are OAuth protected, the HS256 shared secret used to sign the JWT generated by the [Authorization Server](https://github.com/ibm-garage-ref-storefront/auth-ms-spring) is needed to validate the access token provided by the caller.

However, if you must create your own 2048-bit secret, one can be generated using the following command:

```
cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 256 | head -n 1 | xargs echo -n
```

Note that if the [Authorization Server](https://github.com/ibm-garage-ref-storefront/auth-ms-spring) is also deployed, it must use the same HS256 shared secret.

- To run the test cases for the orders application, run the below command.

```
appsody test --docker-options "-e MYSQL_HOST=host.docker.internal -e MYSQL_PORT=3306 -e MYSQL_DATABASE=ordersdb -e MYSQL_USER=dbuser -e MYSQL_PASSWORD=password -e HS256_KEY=<Paste HS256 key here>"
```

- To run the orders application, run the below command.

```
appsody run --docker-options "-e MYSQL_HOST=host.docker.internal -e MYSQL_PORT=3306 -e MYSQL_DATABASE=ordersdb -e MYSQL_USER=dbuser -e MYSQL_PASSWORD=password -e HS256_KEY=<Paste HS256 key here>"
```

- If it is successfully running, you will see something like below.

```
[Container] 2020-05-07 08:57:32.703  INFO 176 --- [  restartedMain] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
[Container] 2020-05-07 08:57:32.787  WARN 176 --- [  restartedMain] aWebConfiguration$JpaWebMvcConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
[Container] 2020-05-07 08:57:32.835  INFO 176 --- [  restartedMain] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page: class path resource [public/index.html]
[Container] 2020-05-07 08:57:33.385  INFO 176 --- [  restartedMain] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 4 endpoint(s) beneath base path '/actuator'
[Container] 2020-05-07 08:57:33.504  INFO 176 --- [  restartedMain] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path '/micro'
[Container] 2020-05-07 08:57:33.507  INFO 176 --- [  restartedMain] application.Main                         : Started Main in 11.91 seconds (JVM running for 14.039)
```

- You can also verify it as follows.

```
$ docker ps
CONTAINER ID        IMAGE                            COMMAND                  CREATED              STATUS              PORTS                                                                                              NAMES
98af4c22269f        kabanero/java-spring-boot2:0.3   "/.appsody/appsody-c…"   About a minute ago   Up About a minute   0.0.0.0:5005->5005/tcp, 0.0.0.0:8080->8080/tcp, 0.0.0.0:8443->8443/tcp, 0.0.0.0:35729->35729/tcp   orders-ms-spring
ae5ed47cb0be        mariadb                          "docker-entrypoint.s…"   3 hours ago          Up 3 hours          0.0.0.0:3306->3306/tcp                                                                             ordersmysql
```

### Validating the application

#### Set Up

- To validate the application, we need `HS256` shared secret to generate JWT token.

To make things easier for you, we pasted below the 2048-bit secret that's included in the app, which you can export to your environment as follows:

```
export HS256_KEY="E6526VJkKYhyTFRFMC0pTECpHcZ7TGcq8pKsVVgz9KtESVpheEO284qKzfzg8HpWNBPeHOxNGlyudUHi6i8tFQJXC8PiI48RUpMh23vPDLGD35pCM0417gf58z5xlmRNii56fwRCmIhhV7hDsm3KO2jRv4EBVz7HrYbzFeqI45CaStkMYNipzSm2duuer7zRdMjEKIdqsby0JfpQpykHmC5L6hxkX0BT7XWqztTr6xHCwqst26O0g8r7bXSYjp4a"
```

As the APIs in this microservice are OAuth protected, the HS256 shared secret used to sign the JWT generated by the [Authorization Server](https://github.com/ibm-garage-ref-storefront/auth-ms-spring) is needed to validate the access token provided by the caller.

However, if you must create your own 2048-bit secret, one can be generated using the following command:

```
cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 256 | head -n 1 | xargs echo -n
```

Note that if the [Authorization Server](https://github.com/ibm-garage-ref-storefront/auth-ms-spring) is also deployed, it must use the same HS256 shared secret.

- Now generate a JWT Token with a `blue` scope, which will let you create/get/delete orders.

To do so, run the commands below:

```
# JWT Header
jwt1=$(echo -n '{"alg":"HS256","typ":"JWT"}' | openssl enc -base64);
# JWT Payload
jwt2=$(echo -n "{\"scope\":[\"blue\"],\"user_name\":\"admin\"}" | openssl enc -base64);
# JWT Signature: Header and Payload
jwt3=$(echo -n "${jwt1}.${jwt2}" | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# JWT Signature: Create signed hash with secret key
jwt4=$(echo -n "${jwt3}" | openssl dgst -binary -sha256 -hmac "${HS256_KEY}" | openssl enc -base64 | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# Complete JWT
jwt=$(echo -n "${jwt3}.${jwt4}");
```

Where:

- `blue` is the scope needed to create the order.
- `${HS256_KEY}` is the 2048-bit secret from the previous step.

#### Validation

Now, you can validate the application as follows.

* Create an Order

Run the following to create an order for the `admin` user. Be sure to use the JWT retrieved from the previous step in place of `${jwt}`.

```
curl -i -H "Content-Type: application/json" -H "Authorization: Bearer ${jwt}" -X POST -d '{"itemId":13401, "count":1}' "http://<orders_host>:<orders_port>/micro/orders"
```

If successfully created, you will see something like below.

```
$ curl -i -H "Content-Type: application/json" -H "Authorization: Bearer ${jwt}" -X POST -d '{"itemId":13401, "count":1}' "http://localhost:8080/micro/orders"
HTTP/1.1 201
Location: http://localhost:8080/micro/orders/2c91808371ee5aa50171ee653f440000
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
$ curl -H "Authorization: Bearer ${jwt}" "http://localhost:8080/micro/orders"
[{"id":"2c91808371ee5aa50171ee653f440000","date":"2020-05-07T09:09:04.000+0000","itemId":13401,"customerId":"admin","count":1}]
```

- Also you can access the swagger ui at http://localhost:8080/micro/swagger-ui.html

![Orders Swagger UI](static/swagger_orders.png?raw=true)

- We also enabled sonarqube as part of the application.

To run the sonarqube as a docker container, run the below command.

```
docker run -d --name sonarqube -p 9000:9000 sonarqube
```

To test the application, run the below command.

```
./mvnw sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

Now, access `http://localhost:9000/`, login using the credentials `admin/admin`, and then you will see something like below.

![Orders SonarQube](static/orders_sonarqube.png?raw=true)

- We included contract testing as part of our application too.

To run Pact as a docker container, run the below command.

```
cd pact_docker/
docker-compose up -d
```

To publish the pacts to pacts broker, run the below command.

```
./mvnw clean install pact:publish -Dpact.broker.url=http://localhost:8500 -Ppact-consumer
```

To verify the results, run the below command.

```
 ./mvnw test -Dpact.verifier.publishResults='true' -Dpactbroker.host=localhost -Dpactbroker.port=8500 -Ppact-producer
```

Now you can access the pact broker to see if the tests are successful at http://localhost:8500/.

![Orders Pact Broker](static/orders_pactbroker.png?raw=true)

### Exiting the application

To exit the application, just press `Ctrl+C`.

It shows you something like below.

```
[Container] [INFO] ------------------------------------------------------------------------
[Container] [INFO] BUILD SUCCESS
[Container] [INFO] ------------------------------------------------------------------------
[Container] [INFO] Total time:  20:35 min
[Container] [INFO] Finished at: 2020-05-07T09:17:39Z
[Container] [INFO] ------------------------------------------------------------------------
Closing down development environment.
```

## Conclusion

You have successfully deployed and tested the Orders Microservice and a MariaDB database in local Docker Containers using Appsody.

To see the Orders application working in a more complex microservices use case, checkout our Microservice Reference Architecture Application [here](https://github.com/ibm-garage-ref-storefront/docs).
