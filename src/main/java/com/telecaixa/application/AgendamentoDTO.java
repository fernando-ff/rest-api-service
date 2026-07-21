package com.telecaixa.application;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class AgendamentoDTO {

    private String data;
    private String hora;
    private String servico;

    public AgendamentoDTO() {
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getServico() {
        return servico;
    }

    public void setServico(String servico) {
        this.servico = servico;
    }

    @Override
    public String toString() {
        return "AgendamentoDTO{" +
                "data='" + data + '\'' +
                ", hora='" + hora + '\'' +
                ", servico='" + servico + '\'' +
                '}';
    }
}