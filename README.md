# Tozny End-to-End Encrypted Database

The Tozny End-to-End Encrypted Database (E3DB) is a storage
platform with powerful sharing and consent management features. [Read more on our blog](https://tozny.com/blog/announcing-project-e3db-the-end-to-end-encrypted-database/).

Tozny's E3DB provides a familiar JSON-based NoSQL-style API for reading,
writing, and listing JSON data stored securely in the cloud.

First a caveat: *Don’t store anything you want to keep (yet!).*
This is a developer preview, and we might have to delete the
database periodically. Please do try it out as an experiment,
and over time, we will build it into a durable encrypted storage
solution!


## Quick Start

Please try out E3DB and give us feedback! Read below for detailed
instructions. Here are the basic steps. You'll need Java 8 or higher.
E3DB has been tested on MacOS, Windows, and Linux:

 1. Download [e3db-0.7.0.zip](https://github.com/tozny/e3db/releases/download/0.7.0/e3db-0.7.0.zip) and unzip it. Or clone this repo if you want extra fun.
 1. The `e3db` binary is now in `./e3db-0.7.0/bin`. Add that to your path.
 1. `e3db register` - then check your email!
 1. `e3db ls` - You should see nothing
 1. Write a record: `recordID=$(e3db write address_book '{"name": "John Doe", "phone": "503-555-1212"}')`
 1. `e3db ls` - You should see your new record
 1. Read a record: `e3db read $recordID`
 1. Run `e3db feedback` as many times as you like! This shares end-to-end encrypted feedback with our CEO

## Terms of Service

Your use of E3DB must abide by our [Terms of Service](terms.pdf), as detailed in
the linked document.

# Installation & Use

The Tozny E3DB software contains the following components:

- A Command Line Interface (CLI) tool used for registering
  accounts and performing interactive database operations. To use this
  you will need the JVM.

- A Java 8 or later JRE for connecting to E3DB and performing database
  operations from Java applications or web services. (Java 7 in
  general works, but certain versions of OpenJDK 7 do not support a
  recent enough SSL/TLS to use E3DB.)

To obtain the E3DB CLI binary, download [e3db-0.7.0.zip](https://github.com/tozny/e3db/releases/download/0.7.0/e3db-0.7.0.zip).
You can always find the latest binaries on our releases page at
https://github.com/tozny/e3db/releases.

When you unzip this file, the E3DB CLI executable will be located at
`e3db-0.7.0/bin/e3db`. For ease of use, add this directory to your
path. For example:

    $ unzip e3db-0.7.0.zip -d $HOME
    $ export PATH=$PATH:$HOME/e3db-0.7.0/bin

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

This will prompt interactively for your e-mail address:

```
E-Mail Address []: foo@example.com<RETURN>
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

# Code Examples

To obtain the source for the E3DB CLI and example code, clone
the `e3db` repository from GitHub using the URL `https://github.com/tozny/e3db`.
Once the repository is cloned, you can find Java examples in
the `examples/src/main/java` directory.

Javadocs for the E3DB SDK are available at https://tozny.github.io/e3db-client/.

## Building the Example Code

To build the E3DB code examples with SBT, run (in the root of the repository directory):

    $ ./sbt examples/compile
    $ ./sbt examples/run

If you like, you can provide config information on the command line
which you can get from the info command.

    $ e3db info
    $ ./sbt "examples/run your_client_id your_key_id your_api_secret"

Now go take a look at the example code and play around with it.

## Creating an E3DB Client

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

In the example code, parameters like `clientId` come from the config file
or command-line arguments. In a production system, they would come from a
secure credential storage system, or some other location.

## Writing Records
Now that the client is configured, we can write a record into E3DB. It will
be encrypted automatically and uploaded to the database.

- This starts with constructing metadata about the record itself; the writer
  ID and client ID, as well as the content type.
- The "feedback" content type is what we use for sharing end-to-end encrypted
  feedback about E3DB itself :)
- The map from String to String is the set of name/value pairs that are
  converted to JSON. The values are encrypted and written to E3DB, all transparently.

```java
Meta writeMeta = new Meta(clientId, clientId, "feedback");
HashMap <String, String> map = new HashMap();
map.put("comment", "Hello World! I successfully ran the example file.");
Record nameRecord = new Record(writeMeta, map);
UUID newWriteId = client.writeRecord(nameRecord);
System.out.println("Feedback Created: " + newWriteId);
```

## Reading Records
In the above example, we received a record ID after writing the map to
E3DB. Using this record ID, we can read the map back out. It gets
downloaded and transparently decrypted and displayed on the command line.

```java
Record feedbackRecord2 = client.readRecord(feedbackRecordId).get();
System.out.println ("Read the comment: " + feedbackRecord2.data.get("comment"));
```

There's also a function `client.readRawRecord(id)`` that fetches, but does not decrypt
the data, so you can see the encoded ciphertext if you like.

## Sharing Records

Currently, sharing is facilitated by content type. Our sharing model allows
the other party to read all records of the selected content type; we may add a more
flexible sharing model in the near future. When you share data with another party,
some crypto happens in the client to allow that party to read it, but without any
other party having access, including us.

In the following example, we share the record that we created above with the `UUID` of
Tozny's CEO Isaac.

```java
UUID ipjId = UUID.fromString ("166ed61a-3a56-4fe6-980f-2850aa82ea25");
client.authorizeReader(clientId, ipjId, "feedback");
PolicyRequest shareReq = new PolicyRequest(clientId,
        clientId,
        ipjId,
        Policy.allow(Policy.READ),
        "feedback"
);
client.setPolicy(shareReq);
```

## Listing Records

It is simple to list records visible to the client by calling `listRecords`:

```java
for (Meta meta : client.listRecords(100, 0)) {
  System.out.printf("%-40s %s\n", meta.record_id, meta.type);
}
```

For each record returned by `listRecords`, we receive an instance of `Meta`,
which contains the following meta-information about each record in the
data store. The `record_id` is particularly interesting, because that's
what you use for `readRecord`. For now, write and user IDs are more-or-less
the same, but that will change in future versions, where you can read and
write data about other users.


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

