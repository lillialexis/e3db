# Tozny End-to-End Encrypted Database

The Tozny End-to-End Encrypted Database (E3DB) is a storage
platform with powerful sharing and consent management features.

Tozny's E3DB provides a familiar JSON-based NoSQL-style API for reading,
writing, and listing JSON data stored securely in the cloud.

## Installation

The Tozny E3DB software contains the following components:

- A Command Line Interface (CLI) tool used for registering
  accounts and performing interactive database operations.

- A Java SDK for connecting to E3DB and performing
  database operations from Java applications or web services.

To install the E3DB CLI, download the software from:

    https://github.com/tozny/e3db/releases/download/0.5.0/e3db-cli-0.5.0.zip

Unzip the `e3db-cli-0.5.0.zip` file in a convenient location and add
the `e3db-cli-0.5.0/bin` directory to your `PATH`.

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
configuration will be written to the file `$HOME/.tozny/e3db.json`.

## CLI Examples

These examples demonstrate how to use the E3DB Command Line
Interface to interactively use E3DB as a database without
the need to write any code.

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
$ ./e3db ls
Record ID                                 Producer      Type
------------------------------------------------------------------------------
874b41ff-ac84-4961-a91d-9e0c114d0e92      e04af806...   address_book
```

For each record accessible in E3DB, the `ls` command lists the record ID,
ID of the client that wrote the record, and the type of data contained
in the record.

When other parties share data with us, the `Producer` column will show the
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

The Tozny E3DB stores each field encrypted using 256-bit AES
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
$ e3eb share eb540605-9f2f-4251-bd40-90ba8da99615 address_book
```

This command will set up access control policy to allow the
client with the unique ID `eb540605-9f2f-4251-bd40-90ba8da99615`
to read your records with type `address_book`. It will also
securely share the encryption key for those records with the
client so they can decrypt the contents of each field.

## Code Examples

### Importing the E3DB Client Library

### Creating a E3DB Client

### Writing Records

### Listing Records

### Reading Records

### Sharing Records

