variable "compartment_ocid" {
  type        = string
  description = "Your OCI Compartment OCID"
}

variable "ssh_public_key" {
  type        = string
  description = "SSH public key for instance access"
  default     = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIDy59cGcMIHU2s59ddvOMWV9m/BNkLWZIoU9492hMs5e fferreiralf.1@gmail.com"
}

variable "duckdns_domain" {
  type        = string
  description = "Free DuckDNS subdomain, for example: myapp.duckdns.org"
  default     = ""
}

variable "duckdns_token" {
  type        = string
  description = "DuckDNS token used to update the A record"
  default     = ""
}

variable "letsencrypt_email" {
  type        = string
  description = "Email used to request the free Let's Encrypt certificate"
  default     = ""
}