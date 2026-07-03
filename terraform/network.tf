# ------------------------------------------------------------------------------
# VIRTUAL CLOUD NETWORK (VCN)
# ------------------------------------------------------------------------------
resource "oci_core_vcn" "quarkus_vcn" {
  cidr_block     = "10.0.0.0/16"
  compartment_id = var.compartment_ocid
  display_name   = "quarkus-vcn"
  dns_label      = "quarkusvcn"
}

# ------------------------------------------------------------------------------
# INTERNET GATEWAY
# ------------------------------------------------------------------------------
resource "oci_core_internet_gateway" "quarkus_ig" {
  compartment_id = var.compartment_ocid
  display_name   = "quarkus-internet-gateway"
  vcn_id         = oci_core_vcn.quarkus_vcn.id
}

# ------------------------------------------------------------------------------
# ROUTE TABLE
# ------------------------------------------------------------------------------
resource "oci_core_default_route_table" "quarkus_rt" {
  manage_default_resource_id = oci_core_vcn.quarkus_vcn.default_route_table_id

  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.quarkus_ig.id
  }
}

# ------------------------------------------------------------------------------
# SECURITY LIST (FIREWALL)
# ------------------------------------------------------------------------------
resource "oci_core_security_list" "quarkus_custom_sl" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.quarkus_vcn.id
  display_name   = "quarkus-custom-security-list"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
    stateless   = false
  }

  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    stateless   = false
    description = "Allow SSH Management"
    
    tcp_options {
      min = 22
      max = 22
    }
  }

  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    stateless   = false
    description = "Allow Quarkus Application Port"
    
    tcp_options {
      min = 8080
      max = 8080
    }
  }
}

# ------------------------------------------------------------------------------
# PUBLIC SUBNET
# ------------------------------------------------------------------------------
resource "oci_core_subnet" "quarkus_subnet" {
  cidr_block        = "10.0.1.0/24"
  compartment_id    = var.compartment_ocid
  vcn_id            = oci_core_vcn.quarkus_vcn.id
  display_name      = "quarkus-subnet"
  dns_label         = "quarkussubnet"
  security_list_ids = [oci_core_security_list.quarkus_custom_sl.id]
}