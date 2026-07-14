variable "aws_region" {
  description = "AWS region used for all project resources."
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Prefix used for AWS resource names."
  type        = string
  default     = "patient-management"
}
