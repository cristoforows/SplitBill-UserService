package SplitBill.UserService.UserService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;


public class UserDetailsVerticle extends AbstractVerticle {

  boolean isRegistered;
  boolean isPhoneNumberRegistered;

  @Override
  public void start() {
    isRegistered = false;
    isPhoneNumberRegistered = false;

    vertx.eventBus().consumer("createUser.handler.addr", message -> {
      JsonObject payload = new JsonObject(message.body().toString());

      String email = payload.getString("email");
      String phone_number = payload.getString("phone_number");
      String name = payload.getString("name");
      String profile_image = payload.getString("profile_image");

      PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(5432)
        .setHost("db.lboyxtcstmipodqcbzam.supabase.co")
        .setDatabase("postgres")
        .setUser("postgres")
        .setPassword("s96!rivFln%4KaRT");

      // Pool options
      PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(10);

      // Create the pooled client
      PgPool pool = PgPool.pool(vertx, connectOptions, poolOptions);

      // find if phoneNumber already existed
      pool.getConnection().
        compose(connection ->
          connection
            .preparedQuery("SELECT id, is_registered FROM public.\"Users\" WHERE phone_number = '" + phone_number + "'")
            .execute()
            .compose(result -> {
              if (result.size() == 0) {
                isRegistered = false;
                isPhoneNumberRegistered = false;
              }
              for (Row row : result) {
                isPhoneNumberRegistered = true;
                isRegistered = row.getBoolean("is_registered");
              }
              String query;
              if (isRegistered && isPhoneNumberRegistered) {
                message.fail(400, "User already existed");
                throw new RuntimeException("User already existed");
              } else if (!isRegistered && isPhoneNumberRegistered) {
                //update user
                query = "UPDATE public.\"Users\" SET email = '" + email + "', name = '" + name + "', profile_image = '" + profile_image + "', is_registered = true WHERE phone_number = '" + phone_number + "'";
              } else {
                //create user
                query = "INSERT INTO public.\"Users\" (email, phone_number, name, profile_image, is_registered) VALUES ('" + email + "', '" + phone_number + "', '" + name + "', '" + profile_image + "', true)";
              }
              return connection
                .preparedQuery(query)
                .execute()
                .compose(res2 -> connection
                  .query("SELECT id FROM public.\"Users\" WHERE phone_number = '" + phone_number + "' and email = '" + email + "'")
                  .execute())
                .onComplete(ar ->
                  connection.close())
                .onSuccess(rows -> {
                  for (Row row : rows) {
                    message.reply(new JsonObject().put("user_id", row.getUUID("id").toString()));
                  }
                });
            })
        );

    });
  }

}

