# Tozny End-to-End Encrypted Database

The Tozny End-to-End Encrypted Database (E3DB) is a storage
platform with powerful sharing and consent management features.

Tozny's E3DB provides a familiar JSON-based NoSQL-style API for reading,
writing, and listing JSON data stored securely in the cloud.

## Quick Start

Please try out E3DB and give us feedback! Here are the basic steps:

 1. [Download](https://github.com/tozny/e3db/releases/download/0.5.1/e3db-0.5.1.zip) and unzip E3DB.
 1. The e3db binary is now in ./e3db-0.5.1/bin/e3db
 1. e3db register # then check your email!
 1. e3db ls # You should see nothing
 1. Write a record: recordID=$(e3db write address_book '{"name": "John Doe", "phone": "503-555-1212"}')
 1. e3db ls # You should see your new record
 1. Read a record: e3db read $recordID
 1. Now try the same thing on a different account and share the data as below.
 1. Review and run the feedback.sh script to tell us your thoughts. You'll have to set your path at the top.

## Terms of Service

Your use of E3DB must abide by our [Terms of Service](terms.pdf), as detailed in
the linked document.

## Installation

(Note: These install instructions contain examples for Mac OS
and Linux users. The process is similar on Windows---build steps
for Windows users will be provided in another document in a future
release.)

The Tozny E3DB software contains the following components:

- A Command Line Interface (CLI) tool used for registering
  accounts and performing interactive database operations.

- A Java SDK for connecting to E3DB and performing
  database operations from Java applications or web services.

To obtain the source for the E3DB CLI and example code, check
out the Git repository by running:

    $ git clone https://github.com/tozny/e3db
    $ cd e3db

Next, compile and package the E3DB CLI using SBT. This will automatically
fetch the E3DB Client Library from our Maven repository:

    $ ./sbt universal:packageBin

(Note that it will take some time to install the Scala compiler and SBT
runtime the first time the `sbt` script is run.)

When this completes, the binary distribution will be located in:

    target/universal/e3db-0.5.0.zip

Unzip this file to any location and add this directory to your
path. For example:

    $ unzip target/universal/e3db-0.5.0.zip -d $HOME
    $ export PATH=$PATH:$HOME/e3db-0.5.0/bin

You should now be able to run the E3DB CLI via the `e3db` command:

    $ e3db --help
    Usage: e3db [-v|--verbose] [-c|--config FILENAME] COMMAND
    [...]

## Registration

Before you can use E3DB from your Java application, you must
register an account and receive API credentials. To register using
the Tozny E3DB CLI, run:

```
$ e3db register
```

This will prompt interactively for your e-mail address (and the
server URL---accept the default by pressing return):

```
E-Mail Address []: foo@example.com<RETURN>
Service URL [https://api.e3db.tozny.com/v1]: <RETURN>
```

Tozny will send a confirmation e-mail to the address entered
during the registration process. Simply click the link in the
e-mail to complete the registration.

After a successful registration, API credentials and other
configuration will be written to the file `$HOME/.tozny/e3db.json`
and can be displayed by running `e3db info`.

## CLI Examples

These examples demonstrate how to use the E3DB Command Line
Interface to interactively use E3DB as a database without
the need to write any code.

Note that all E3DB commands have help, so anytime you can see
the documentation for a given command using the `--help` argument. For
example, you can see help on all commands:

```
$ e3db --help
...
```

Or help on a particular command, such as `register`:

```
$ e3db register --help
...
```

### Writing Records

To write a record containing free-form JSON data, use the
`e3db write` subcommand. Each record is tagged with a "content
type", which is a string that you choose used to identify the
structure of your data.

In this example, we write an address book entry into E3DB:

```
$ e3db write address_book '{"name": "John Doe", "phone": "503-555-1212"}'
874b41ff-ac84-4961-a91d-9e0c114d0e92
```

Once E3SB has written the record, it outputs the Universally Unique
Identifier (UUID) of the newly created data. This can be used later
to retrieve the specific record.

You can also write a file containing JSON data using the following
command syntax:

```
$ e3db write address_book @contact1.json
```

### Listing Records

To list all records that we have access to in E3DB, use the
`e3db ls` command:

```
$ e3db ls
Record ID                                 Writer        Type
------------------------------------------------------------------------------
874b41ff-ac84-4961-a91d-9e0c114d0e92      e04af806...   address_book
```

For each record accessible in E3DB, the `ls` command lists the record ID,
ID of the client that wrote the record, and the type of data contained
in the record.

When other parties share data with us, the `Writer` column will show the
ID of the client that shared the data, rather than our own client ID.

### Reading Records

To read a record, we must first know the unique record ID. This was
printed by the CLI when the record was first written, and also displayed
in the output of the `ls` command.

```
$ e3db read 874b41ff-ac84-4961-a91d-9e0c114d0e92

Record ID:           874b41ff-ac84-4961-a91d-9e0c114d0e92
Record Type:         address_book
Writer ID:           e04af806-3bbf-41d6-9ed1-a12bdab671ee
User ID:             e04af806-3bbf-41d6-9ed1-a12bdab671ee

Field                Value
------------------------------------------------------------------------------
phone                503-555-1212
name                 John Doe
```

The `read` command first prints meta-information about the record,
such as its unique ID, type, and the IDs of the client that wrote
the record and its associated user. Then it displays each field
from the original JSON data, along with its decrypted value.

The Tozny E3DB stores each field encrypted using 128-bit AES
encryption with a unique key for each client, user, and
data type. Normally, the E3DB client performs this encryption
and decryption transparently. To skip this decryption step and
retrieve the raw encrypted record data, add the `--raw` option
to `e3db read`:

```
$ e3db read --raw 874b41ff-ac84-4961-a91d-9e0c114d0e92

Record ID:           874b41ff-ac84-4961-a91d-9e0c114d0e92
Record Type:         address_book
Writer ID:           e04af806-3bbf-41d6-9ed1-a12bdab671ee
User ID:             e04af806-3bbf-41d6-9ed1-a12bdab671ee

Field                Value
------------------------------------------------------------------------------
name                 eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0..aydiGjC7GpQKj_cg.vOE0lnaOnRA.-kY2TzM2GHBGHXaiuVRDxg
phone                eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0..LKSzjq5l3iDOVy-M.8-6BC4yXdx5gHrSO.zONcqjo7E1wpcsRZkgsE0w
```

### Sharing Records

The Tozny E3DB allows you to share your data with another E3DB
client. Sharing records allows your records to show in the
record list for another client. In order to set up sharing,
you must know the unique ID of the client you wish to share
with. If the other client is using the E3DB CLI, you can
retrieve this by running `e3db info`.

The current version of the E3DB client allows you to share
records based on their content type. For example, to share
all address book entries with another client:

```
$ e3db share address_book eb540605-9f2f-4251-bd40-90ba8da99615
```

This command will set up access control policy to allow the
client with the unique ID `eb540605-9f2f-4251-bd40-90ba8da99615`
to read your records with type `address_book`. It will also
securely share the encryption key for those records with the
client so they can decrypt the contents of each field.

### Storing Files

The E3DB CLI has special support for writing files to E3DB. To
write a file, use the `writefile` command:

```
$ e3db writefile DOC message.txt
eee7bb6f-90f3-407f-a6f7-08923c0c64d5
```

The first argument is the "content type" of the file (with the same meaning
as the content type argument given to the `write` and `share` commands), while
the second is a path to the file.

The UUID printed on the console is the record ID for the file in E3DB.

### Retrieving Files

A previously stored file can be retrieved using `readfile`. Assuming
the UUID above:

```
$ e3db readfile eee7bb6f-90f3-407f-a6f7-08923c0c64d5
```

which would write a file named `message.txt` in the current directory.

## Code Examples

The `e3db` repository also contains example code showing simple
Java code using the E3DB client library. The Java example code
lives at:

    examples/src/main/java

### Building the Example Code

To build the E3DB code examples with SBT, run:

    $ ./sbt examples/compile

and to run it, you'll need to provide some information on the command line
which you can get from the info command.

    $ e3db info
    $ ./sbt "examples/run your_client_id your_key_id your_api_secret"

Now go take a look at the example code and play around with it.

### Creating an E3DB Client

Each example class begins by creating a `Client` object via the
class `HttpE3DBClientBuilder`. Using a builder-style interface
makes it easy to pass in the necessary configuration settings and
credentials needed to set up the client.

```java
Client client = new HttpE3DBClientBuilder()
  .setClientId(clientId)
  .setApiKeyId(apiKeyId)
  .setApiSecret(apiSecret)
  .setKeyManager(keyManager)
  .setServiceUri("https://api.e3db.tozny.com/v1")
  .build();
```

In the example code, parameters like `clientId` come from command-line
arguments. In a production system, they would come from a configuration
file, credential storage system, or some other location.

### Writing Records

### Listing Records

Once the client API is configured, it is simple to list records visible
to the client by calling `listRecords`:

```java
for (Meta meta : client.listRecords(100, 0)) {
  System.out.printf("%-40s %s\n", meta.record_id, meta.type);
}
```

For each record returned by `listRecords`, we receive an instance
of `Meta`, which contains the following meta-information about each
record in the data store:

```java
public class Meta {
  public final UUID record_id;
  public final UUID writer_id;
  public final UUID user_id;
  public final String type;
  public final Date created;
  public final Date last_modified;
}
```

### Reading Records

### Sharing Records
