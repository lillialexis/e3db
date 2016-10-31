# Tozny Personal Data Service

The Tozny Personal Data Service (PDS) is an end-to-end encrypted
storage platform with powerful sharing and consent management
features.

Tozny's PDS provides a familiar JSON-based NoSQL-style API for reading,
writing, and listing JSON data stored securely in the cloud.

## Installation

The Tozny PDS software contains the following components:

- A Command Line Interface (CLI) tool used for registering
  accounts and performing interactive database operations.

- A Java SDK for connecting to the PDS and performing
  database operations from Java applications or web services.

To install the PDS CLI, download the software from:

    https://github.com/tozny/pds/releases/download/0.4.1/pds-cli-0.4.1.zip

Unzip the `pds-cli-0.4.1.zip` file in a convenient location and add
the `pds-cli-0.4.1.zip/bin` directory to your `PATH`.

## Registration

Before you can use the PDS from your Java application, you must
register an account and receive API credentials. To register using
the Tozny PDS CLI, run:

```
$ pds register
```

This will prompt interactively for your e-mail address (and the
server URL---accept the default by pressing return):

```
E-Mail Address []: foo@example.com<RETURN>
Service URL [https://api.staging.pds.tozny.com/v1]: <RETURN>
```

Tozny will send a confirmation e-mail to the address entered
during the registration process. Simply click the link in the
e-mail to complete the registration.

After a successful registration, API credentials and other
configuration will be written to the file `$HOME/.tozny/pds.json`.

## CLI Examples

These examples demonstrate how to use the PDS Command Line
Interface to interactively use the PDS as a database without
the need to write any code.

### Writing Records

To write a record containing free-form JSON data, use the
`pds write` subcommand. Each record is tagged with a "content
type", which is a string that you choose used to identify the
structure of your data.

In this example, we write an address book entry into our PDS:

```
$ pds write address_book '{"name": "John Doe", "phone": "503-555-1212"}'
874b41ff-ac84-4961-a91d-9e0c114d0e92
```

Once the PDS has written the record, it outputs the Universally Unique
Identifier (UUID) of the newly created data. This can be used later
to retrieve the specific record.

You can also write a file containing JSON data using the following
command syntax:

```
$ pds write address_book @contact1.json
```

### Listing Records

To list all records that we have access to in our PDS, use the
`pds ls` command:

```
$ ./pds ls
Record ID                                 Producer      Type
------------------------------------------------------------------------------
874b41ff-ac84-4961-a91d-9e0c114d0e92      e04af806...   address_book
```

For each record accessible in our PDS, the `ls` command lists the record ID,
ID of the client that wrote the record, and the type of data contained
in the record.

When other parties share data with us, the `Producer` column will show the
ID of the client that shared the data, rather than our own client ID.

### Reading Records

To read a record, we must first know the unique record ID. This was
printed by the CLI when the record was first written, and also displayed
in the output of the `ls` command.

```
$ pds read 874b41ff-ac84-4961-a91d-9e0c114d0e92

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

The Tozny PDS stores each field encrypted using 256-bit AES
encryption with a unique key for each client, user, and
data type. Normally, the PDS client performs this encryption
and decryption transparently. To skip this decryption step and
retrieve the raw encrypted record data, add the `--raw` option
to `pds read`:

```
$ pds read --raw 874b41ff-ac84-4961-a91d-9e0c114d0e92

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

The Tozny PDS allows you to share your data with another PDS
client. Sharing records allows your records to show in the
record list for another client. In order to set up sharing,
you must know the unique ID of the client you wish to share
with. If the other client is using the PDS CLI, you can
retrieve this by running `pds info`.

The current version of the PDS client allows you to share
records based on their content type. For example, to share
all address book entries with another client:

```
$ pds share eb540605-9f2f-4251-bd40-90ba8da99615 address_book
```

This command will set up access control policy to allow the
client with the unique ID `eb540605-9f2f-4251-bd40-90ba8da99615`
to read your records with type `address_book`. It will also
securely share the encryption key for those records with the
client so they can decrypt the contents of each field.

## Code Examples

### Importing the PDS Client Library

### Creating a PDS Client

### Writing Records

### Listing Records

### Reading Records

### Sharing Records

