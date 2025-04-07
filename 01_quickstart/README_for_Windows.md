# Lessons learned

- The executables for MS Windows can be found under `bin/windows/` in the Kafka installation
- When using the regular Windows command shell, the length of a command must not exceed a certain length (128 chars?),
  otherwise the terminal simply won't execute the command. It might be necessary to move the Kafka installation to a
  location with a shorter path.
- If a user does not have full disk access, it might be necessary to update the `log.dirs` property in
  `server.properties` and point it to some folder in the user's home dir
- It might be necessary to change the value of `listeners` in `server.properties` to `PLAINTEXT://127.0.0.1:9092`
- When working with Kafka Streams, an internal instance of RocksDB will be started. This might fail when using an
  outdated JDK.