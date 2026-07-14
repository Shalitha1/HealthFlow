resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "aws_secretsmanager_secret" "application" {
  name                    = "${var.project_name}/application"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "application" {
  secret_id = aws_secretsmanager_secret.application.id

  secret_string = jsonencode({
    AUTH_DB_HOST     = aws_db_instance.service["auth"].address
    AUTH_DB_PORT     = aws_db_instance.service["auth"].port
    AUTH_DB_NAME     = local.database_services["auth"].db_name
    AUTH_DB_USERNAME = aws_db_instance.service["auth"].username
    AUTH_DB_PASSWORD = random_password.database["auth"].result

    PATIENT_DB_HOST     = aws_db_instance.service["patient"].address
    PATIENT_DB_PORT     = aws_db_instance.service["patient"].port
    PATIENT_DB_NAME     = local.database_services["patient"].db_name
    PATIENT_DB_USERNAME = aws_db_instance.service["patient"].username
    PATIENT_DB_PASSWORD = random_password.database["patient"].result

    AUDIT_DB_HOST     = aws_db_instance.service["audit"].address
    AUDIT_DB_PORT     = aws_db_instance.service["audit"].port
    AUDIT_DB_NAME     = local.database_services["audit"].db_name
    AUDIT_DB_USERNAME = aws_db_instance.service["audit"].username
    AUDIT_DB_PASSWORD = random_password.database["audit"].result

    JWT_SECRET = random_password.jwt.result
  })
}
