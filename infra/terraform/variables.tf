variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "project_name" {
  type    = string
  default = "rugbylive"
}

variable "ssh_allowed_cidr" {
  type        = string
  description = "Your IP in CIDR format, e.g. 1.2.3.4/32"
}

variable "public_key_path" {
  type        = string
  description = "Path to your local SSH public key"
  default     = "~/.ssh/rugbylive.pub"
}

variable "instance_type" {
  type    = string
  default = "t3.micro"
}