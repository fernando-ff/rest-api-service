output "instance_public_ip" {
  value       = oci_core_instance.quarkus_server.public_ip
  description = "The public IP address of your Always Free Quarkus Server."
}