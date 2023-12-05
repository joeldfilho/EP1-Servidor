package com.example.ep1;

public enum Status {
    NOVA("nova"),
    ANDAMENTO("Em andamento"),
    FINALIZADA("Finalizada");

    private String descricao;

    Status(String descricao){
        this.descricao = descricao;
    }

    public String getDescricao(){
        return descricao;
    }
}

