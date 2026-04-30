output "api_public_ip" {
  value = aws_eip.api.public_ip
}

output "api_ecr_repository_url" {
  value = aws_ecr_repository.api.repository_url
}

output "ssh_command" {
  value = "ssh ubuntu@${aws_eip.api.public_ip}"
}