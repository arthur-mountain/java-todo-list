db = db.getSiblingDB(process.env.MONGODB_DB);

db.createUser({
  user: process.env.MONGODB_TEST_USERNAME1,
  pwd: process.env.MONGODB_TEST_PASSWORD1,
  roles: [{ role: process.env.MONGODB_TEST_ROLE1, db: process.env.MONGODB_DB }],
});
