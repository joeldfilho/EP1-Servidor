package com.example.ep1servidor;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Essa classe representa o servidor, ela deve ficar rodando e vai ter um Server Socket que receberá e aceitará as conexões
 * */
public class Servidor {

    static ArrayList<TarefaModel> tarefas = new ArrayList<>();

    private static ServerSocket serverSocket;
    private static StringBuilder retorno;

    /*
    * Esse construtor recebe um ServerSocket como argumento e cnostrói o objeto da calsse, que vai ficar buscando novas conexões. Quando uma nova conexão for solicitada ele irá gerar um novo socket e tratar esse Socket dentro de uma thread própria.
    * */
    public Servidor(ServerSocket serverSocket) throws IOException {
        Servidor.serverSocket = serverSocket;
        while (true) {
            Socket socket = serverSocket.accept();
            ClienteHandler clienteHandler = new ClienteHandler(socket);
            new Thread(clienteHandler).start();
        }
    }

    /**
     * Classe interna responsável por gerenciar as multiplas conexões de clientes ao servidor
     * */
    private static class ClienteHandler implements Runnable {
        private Socket socket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        @FXML
        private VBox vboxMessages;

        private StringBuilder retorno;

        public ClienteHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        @Override
        public void run() {
            try {
                while (socket.isConnected()) {
                    String mensagemDoCliente = bufferedReader.readLine();
                    Controller.addLabel(mensagemDoCliente, vboxMessages);
                    tratarMensagem(mensagemDoCliente);
                    mandarMensagemCliente();
                }
            } catch (IOException e) {
                System.out.println("Erro recebendo mensagem do cliente");
                e.printStackTrace();
            } finally {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }

        public void mandarMensagemCliente(){
            try {
                bufferedWriter.write(retorno.toString());
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e){
                System.out.println("Erro enviando mensagem ao cliente");
                e.printStackTrace();
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }

        private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
            try{
                if (bufferedReader != null){
                    bufferedReader.close();
                }
                if (bufferedWriter != null){
                    bufferedWriter.close();
                }
                if (socket != null){
                    socket.close();
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        public void receberMensagemCliente(VBox vboxMessages) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (socket.isConnected()){
                        try{
                            String mensagemDoCliente = bufferedReader.readLine();
                            Controller.addLabel(mensagemDoCliente, vboxMessages);
                            tratarMensagem(mensagemDoCliente);
                            mandarMensagemCliente();
                        } catch (IOException e){
                            System.out.println("Erro recebendo mensagem do cliente");
                            e.printStackTrace();
                            closeEverything(socket, bufferedReader, bufferedWriter);
                            break;
                        }
                    }
                }
            }).start();
        }

        private void tratarMensagem(String mensagemRecebida) {
            String[] argumentos = mensagemRecebida.split(",");
            switch (argumentos[0].toLowerCase(Locale.ROOT)){
                case "criar" -> criarTarefa(argumentos);
                case "editar" -> editarTarefa(argumentos);
                case "apagar" -> apagarTarefa(argumentos);
                case "listar" -> listarTarefas();
                case "encerrar" -> encerrarTarefa(argumentos);
                default -> solicitarMensagem();
            }
        }

        private void solicitarMensagem() {
            retorno = new StringBuilder();
            retorno.append("Caso deseje criar uma tarefa enviar \"criar, {nome_tarefa}, {descricao}, {prioridade}, {responsavel}\", caso deseje saber as tarefas disponiveis mandar: \"listar\"" +
                    "Caso deseje alterar uma tarefa enviar \"editar,{id_tarefa}, {nome_tarefa}, {descricao}, {prioridade}, {responsavel}\"" +
                    "Caso deseje apagar uma tarefa, enviar \"apagar,{id_tarefa}\" + " +
                    "Caso deseje encerrar uma tarefa enviar \"encerrar,{id_tarefa}\"");
        }

        private void listarTarefas() {
            retorno = new StringBuilder();
            for (TarefaModel tarefa:
                    tarefas) {
                retorno.append(tarefa.getId()).append(", titulo:  ").append(tarefa.getTitulo()).append(", descricao: ").append(tarefa.getDescricao()).append(", status: ").append(tarefa.getStatus()).append(", prioridade: ").append(tarefa.getPrioridade()).append(", responsavel: ").append(tarefa.getResponsavel()).append("\n");
            }
        }

        /*
        * Para apagar uma tarefa a mensagem deve vir com o formato "{funcao}, {id_tarefa}"
        * */
        private void apagarTarefa(String[] argumentos) {
            tarefas.remove(Integer.parseInt(argumentos[1]));
            retorno = new StringBuilder("tarefa com o id " + argumentos[1] + " removida");
        }

        /*
        * Para editar uma tarefa a mensagem deve vir no formato "{funcao},{id_funcao}, {nome_tarefa}, {descricao}, {prioridade}, {responsavel}"
        * */
        private void editarTarefa(String[] argumentos) {
            int idTarefa = Integer.parseInt(argumentos[1]);
            String nomeTarefa = argumentos[2];
            String descricaoTarefa = argumentos[3];
            Status status = Status.ANDAMENTO;
            int prioridade = Integer.parseInt(argumentos[4].replace(" ", ""));
            String responsavel = argumentos[5];
            TarefaModel tarefa = new TarefaModel(idTarefa, nomeTarefa, descricaoTarefa, status, prioridade, responsavel);
            tarefas.remove(idTarefa);
            if (tarefas.isEmpty()){
                tarefas.add(tarefa);
                retorno = new StringBuilder("tarefa com o id " + idTarefa + " alterada");
            }else {
                tarefas.set(idTarefa, tarefa);
                retorno = new StringBuilder("tarefa com o id " + idTarefa + " alterada");
            }
        }

        //A mensagem deve vir no formato "{funcao}, {nome_tarefa}, {descricao}, {prioridade}, {responsavel}"
        private void criarTarefa(String[] argumentos) {
            String nomeTarefa = argumentos[1];
            String descricaoTarefa = argumentos[2];
            Status status = Status.NOVA;
            int prioridade = Integer.parseInt(argumentos[3]);
            String responsavel = argumentos[4];
            TarefaModel tarefa = new TarefaModel(tarefas.size(), nomeTarefa, descricaoTarefa, status, prioridade, responsavel);
            tarefas.add(tarefa);
            retorno = new StringBuilder("tarefa com o id " + (tarefas.size() - 1)  + " criada");
        }

        /*Para encerrar uma tarefa usar o formato "funcao,{id_tarefa}*/
        private void encerrarTarefa(String[] argumetnos){
            int idTarefa = Integer.parseInt(argumetnos[1]);
            TarefaModel tarefa = tarefas.get(idTarefa);
            tarefa.setStatus(Status.FINALIZADA);
            tarefas.remove(idTarefa);
            tarefas.add(idTarefa, tarefa);
        }
    }
}
