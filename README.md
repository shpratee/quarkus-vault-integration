# quarkus-vault-integration
Quarkus and Hashicorp Vault configuration for secret properties management

You can store the secret properties in Hashicorp Vault. Vault is an open source tool for securely accessing secrets.

https://www.vaultproject.io/docs

## Install HashiCorp Vault in local
Follow https://learn.hashicorp.com/tutorials/vault/getting-started-install to install the vault.

## Install HashiCorp Vault in Docker

1. Create the docker container using vault image
```
docker run --rm --cap-add=IPC_LOCK -e VAULT_ADDR=http://localhost:8200 -p 8200:8200 --name=dev-vault vault:1.2.2
```

2. Open shell in container and configure the vault token
```
docker exec -it dev-vault sh
export VAULT_TOKEN=<recieved from the installation in above step>
```

## Install Hashicorp Vault on Kubernetes

1. Add required Helm repository
```
$ helm repo add hashicorp https://helm.releases.hashicorp.com
```

2. Install vault using helm chart
```
$ helm install vault hashicorp/vault
```

The Helm chart default installs Vault in "standalone" mode and if you want to install this in "dev" mode, you would ave to provide an extra parameter.
``
$ helm install vault hashicorp/vault --set "server.dev.enabled=true"
``

3. Port-forward vault pod to 8200 to expose it on localhost and see the UI
``
$ kubectl port-forward vault-0 8200:8200
``

## Configure Vault with some properties to be accessed in application container

1. If Secret Engine 'kv' is not enabled, enable it.
``
$ vault secrets enable -path=secret kv-v2
``

2. Create a secret with name 'foo' in kv secret engine
``
$ vault kv put secret/myapps/vault-service/config foo=bar
``

3. Create a policy to provide read only access to the vault
```
$ cat <<EOF | vault policy write vault-service-policy -
> path "secret/data/myapps/vault-service/*" {
>  capabilities = ["read"]
>}
>EOF
```

4. Enable authentication to vault
```
$ vault auth enable userpass --> enabling username/password authentication regime
$ vault write auth/userpass/users/jelly password="jelly" policies="vault-service-policy"
```

Quarkus supports multiple authentication types:
a. token --> Pass the user token
b. user/password --> Authenticate using username and password credentials
c. approle --> Authenticate using role_id and a secret_id. role_id is usually embedded in Docker container and secret_id is obtained by Kubernetes cluster as cubbyhole.
d. kubrenetes --> Authenticate using Kubernetes Service Account Token

## Register Quarkus application to use Vault

1. Add "quarkus-vault" extension
``
$ ./mvnw quarkus:add-extension -Dextensions="quarkus-vault"
``

2. Customize properties in application-properties to use vault
```
quarkus.vault.url=http://localhost:8200 ---> This is for dev. But if you're running vault in kubernetes and we recommend that to be in a separate namespace, the URL will be "http://<service-name>.<namespace>.cluster.local:8200" or "http://<servicename>:8200"
quarkus.vault.authentication.userpass.username=jelly
quarkus.vault.authentication.userpass.password=jelly

quarkus.vault.kv-secret-engine-version=2
quarkus.vault.secret-config-kv-path=myapps/vault-service/config
```

3. Access secret property using @ConfigProperty annotation
```
@ConfigProperty(name = "foo") 
String foo;
```

## Use vault in Cryptography-as-a-Service mode
To avoid spreading all cryptographic operations across services, use "transit" engine of vault.

The transit secrets engine can also sign and verify data; generate hashes and HMACs of data; and act as a source of random bytes.
https://www.vaultproject.io/docs/secrets/transit

1. Open sh in Vault's container to configure vault and add keys to encrypt and sign messages:
```
$ vault secrets enable transit

$ vault write -f transit/keys/my-encryption-key ---> Usually each application has its own encryption key.
$ vault write transit/keys/my-sign-keytype=ecdsa-p256 
```

2. Create a policy that gives access to transit operations:
```
$ cat <<EOF | vault policy write vault-service-policy -
> path "transit/*" {
>   capabilities = ["create","read","update"]
>}
>EOF 
```

3. Enable credentials
```
$ vault auth enable userpass
$ vault write auth/userpass/users/jelly password="jelly" policies="vault-service-policy"
```

4. Inject the io.quarkus.vault.VaultTransitSecretEngine instance to use transit operations
```
@Inject
VaultTransitSecretEngine transit;

transit.encrypt("my-encryption-key", text-to-encrypt);

transit.decrypt("my-encryption-key", text-to-decrypt).asString();

transit.sign("my-sign-key", text-to-sign);
```

The following operations are supported by the Vault extension:

 - encrypt:
Encrypts a regular string with a Vault key configured in the transit secret engine.

 - decrypt:
Decrypts the encrypted data with the specified key and returns unencrypted data.

 - rewrap:
Reencrypts into a new cipher text a cipher text that was obtained from encryption using an old key version with the last key version.

 - sign:
Signs an input string with the specified key.

 - verifySignature:
Checks that the signature was obtained from signing the input with the specified key.


