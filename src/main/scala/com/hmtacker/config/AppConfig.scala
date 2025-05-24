package com.hmtacker.config


case class AppConfig(
                      database: DatabaseConfig = DatabaseConfig(),
                      server: ServerConfig = ServerConfig()
                    )