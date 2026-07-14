variable "aws_region" {
  description = "AWS region emulated by LocalStack."
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Prefix used for LocalStack resource names."
  type        = string
  default     = "patient-management-local"
}

