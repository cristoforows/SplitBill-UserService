package SplitBill.UserService.UserService;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;

public class FindUserVerticle extends AbstractVerticle {

  @Override
  public void start() {
    vertx.eventBus().consumer("findEmail.handler.addr", message -> {
      String email = message.body().toString();

      // Connect options

      PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(5432)
        .setHost("db.lboyxtcstmipodqcbzam.supabase.co")
        .setDatabase("postgres")
        .setUser("postgres")
        .setPassword("s96!rivFln%4KaRT");

      // Pool options
      PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(5);

      // Create the pooled client
      SqlClient client = PgPool.client(vertx, connectOptions, poolOptions);

      // A simple query
      client
        .query("SELECT id, phone_number FROM public.\"Users\" WHERE email = '" + email + "'")
        .execute()
        .onComplete(ar -> {
          if (ar.succeeded()) {
            RowSet<Row> result = ar.result();
            if (result.size() == 0) {
              message.fail(404, "User not found");
            }
            if(result.size() > 1) {
              message.fail(500, "Database Error");
            }
            for (Row row : result) {

              message.reply(new JsonObject()
                .put("id", row.getUUID("id").toString())
                .put("phoneNumber", row.getString("phone_number")));
            }
          } else {
              message.fail(500, "Database Error");
              throw new RuntimeException(ar.cause());
          }

          // Now close the pool
          client.close();
        });
    });
  }

}
