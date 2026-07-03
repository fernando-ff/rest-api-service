resource "oci_core_instance" "quarkus_server" {
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id      = var.compartment_ocid
  
  # ALTERAÇÃO: Mudando para o formato AMD Micro gratuito
  shape               = "VM.Standard.E2.1.Micro"
  display_name        = "quarkus-free-tier-server"

  # NOTA: O formato E2.1.Micro possui tamanho fixo, não utiliza o bloco shape_config.

  create_vnic_details {
    subnet_id        = oci_core_subnet.quarkus_subnet.id
    assign_public_ip = true
  }

  source_details {
    source_type = "image"
    # ALTERAÇÃO: Filtra para buscar a imagem Ubuntu compatível com AMD (x86_64)
    source_id   = data.oci_core_images.ubuntu_amd.images[0].id
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = base64encode(file("${path.module}/scripts/setup.sh"))
  }
}

data "oci_identity_availability_domains" "ads" {
  compartment_id = var.compartment_ocid
}

# ALTERAÇÃO: Filtro atualizado para buscar imagens x86_64 compatíveis com o formato Micro
data "oci_core_images" "ubuntu_amd" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "22.04"
  shape                    = "VM.Standard.E2.1.Micro" 
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}