variable "compartment_ocid" {
  type        = string
  description = "Your OCI Compartment OCID"
}

variable "ssh_public_key" {
  type        = string
  description = "SSH public key for instance access"
  default     = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIDy59cGcMIHU2s59ddvOMWV9m/BNkLWZIoU9492hMs5e fferreiralf.1@gmail.com"
}