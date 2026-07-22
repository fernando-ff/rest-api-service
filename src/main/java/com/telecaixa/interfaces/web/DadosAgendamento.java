package com.telecaixa.interfaces.web;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record DadosAgendamento(String data, String hora, String servico) {}
