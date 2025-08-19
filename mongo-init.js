/*
  This script is executed when the MongoDB container is initialized.
  It creates a dedicated user and database for our Spring Boot application.
  The environment variables are passed in from the 'compose.yaml' file.
*/

// Switch to the 'admin' database to create the user.
// The root user (MONGO_INITDB_ROOT_USERNAME) is implicitly used for authentication here.
db = db.getSiblingDB('admin');

// Create the application user with read/write permissions on the application's database.
db.createUser({
  user: process.env.MONGO_APP_USER,
  pwd: process.env.MONGO_APP_PASSWORD,
  roles: [
    {
      role: 'readWrite',
      db: process.env.MONGO_DB_NAME,
    },
  ],
});