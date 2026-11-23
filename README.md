# Trabalho Ninja

O Trabalho Ninja é uma aplicação desktop para cadastrar ninjas, gerenciar missões, montar equipes e consultar relatórios. O projeto foi desenvolvido como trabalho interdisciplinar de Programação Orientada a Objetos e Banco de Dados, com Java, Swing e MySQL.

## Requisitos

- **JDK 21** para compilar e executar o programa.
- **MySQL Server 8** instalado e em execução no computador.
- **Uma IDE com suporte a Java**, como Eclipse, IntelliJ IDEA ou VS Code.
- **Uma ferramenta para executar os scripts SQL**, como MySQL Workbench ou o cliente `mysql`.

O driver JDBC do MySQL já está incluído na pasta `lib/`.

## Instalação

### 1. Baixe o projeto

No GitHub, clique em **Code → Download ZIP** e extraia o arquivo. Se preferir usar Git, clone o repositório.

```bash
git clone https://github.com/Lobibun/TrabalhoNinja.git
```

### 2. Prepare o banco de dados

Conecte-se ao MySQL local e abra e execute os scripts abaixo, na ordem indicada. Use uma conta com permissão para criar o banco, as tabelas e as views.

| Ordem | Script | O que faz |
| --- | --- | --- |
| 1 | [`01_criar_tabelas_iniciais.sql`](sql/01_criar_tabelas_iniciais.sql) | Cria o banco `Trabalho_Ninja` e suas tabelas. |
| 2 | [`02_inserir_dados_iniciais.sql`](sql/02_inserir_dados_iniciais.sql) | Preenche as listas de referência e cadastra dados de exemplo em uma base nova. |
| 3 | [`03_criar_views.sql`](sql/03_criar_views.sql) | Cria as consultas usadas na tela **Consultar views**. |

O programa se conecta ao banco `Trabalho_Ninja` em `localhost:3306`. Para uma instalação nova, os três scripts da tabela são suficientes. Os arquivos da pasta `sql/migracoes/` servem para atualizar bancos de versões antigas.

### 3. Configure e execute o projeto Java

1. Abra a pasta baixada na sua IDE ou crie um projeto Java a partir dos arquivos existentes.
2. Selecione o **JDK 21**, configure `src/` como pasta de código-fonte e adicione o arquivo `.jar` da pasta `lib/` ao classpath do projeto.
3. Na configuração de execução, adicione as variáveis de ambiente abaixo. Use uma conta MySQL com permissão para consultar e gravar dados no banco `Trabalho_Ninja`.

   ```dotenv
   MYSQL_USER=seu_usuario_mysql
   MYSQL_PASSWORD=sua_senha_mysql
   ```

4. Execute a classe `Main`, em `src/Main.java`, para abrir a janela principal.

No Eclipse, configure a pasta `src/` e o driver JDBC em **Java Build Path**. Adicione as credenciais na aba **Environment** da configuração de execução da classe `Main`.

Se a configuração de execução da sua IDE carrega variáveis de um arquivo `.env`, copie `.env.example` para `.env` na raiz do projeto e preencha as credenciais. Mantenha o `.env` com os valores reais no seu computador.

## Como usar

### Ninjas

- **Cadastrar e listar** permite cadastrar um ninja ou selecionar um registro da tabela para editar. Use **Novo ninja** para iniciar outro cadastro. Um ninja pode ter zero, uma ou várias naturezas de chakra. Use o botão **+** nas listas que permitem cadastrar opções, como vila, clã e natureza de chakra.
- **Consultar ninjas** permite combinar filtros por ID, nome, vila, clã, rank, natureza de chakra e status. A busca por nome aceita parte do nome.
- Todo ninja começa com o status **Ativo**. Você pode alterar o status na edição do cadastro. A participação em missões é registrada separadamente.

### Missões

- **Gerenciar missões** permite cadastrar, editar e excluir missões. Informe título, rank, vila de origem e status. A descrição é opcional. Ao selecionar uma missão, a tela mostra os ninjas vinculados, suas funções e as datas de participação.
- **Vincular ninja à missão** registra o ninja, a missão, a função e a data de participação no formato `DD/MM/AAAA`. Você também pode abrir esse formulário pelo botão **Vincular ninja** na tela da missão. Na lista de vínculos, selecione um registro para editar a função e a data ou desvincular o ninja.
- Uma missão pode ter vários ninjas, e um ninja pode participar de várias missões. Cada par de ninja e missão pode ser cadastrado uma vez. Missões **Concluídas** mantêm a equipe no histórico e bloqueiam a inclusão e a remoção de integrantes.

| Rank do ninja | Ranks de missão permitidos |
| --- | --- |
| Genin | D e C |
| Chunin | D, C e B |
| Jounin | D, C, B, A e S |
| Kage | A e S |

### Relatórios

- **Totalizadores** gera e salva no MySQL as contagens de ninjas por vila, rank e natureza de chakra, além das contagens de missões por rank e status. Também mostra quantos ninjas distintos estão vinculados a missões e quantas missões estão sem equipe. Um ninja com várias naturezas entra na contagem de cada uma delas. Ninjas sem natureza têm uma linha própria.
- **Consultar views** exibe quatro consultas do banco sobre participações de ninjas em missões, total de ninjas por vila, total de missões por rank e missões sem ninjas vinculados.

## Primeiro teste

1. Abra **Ninjas → Consultar ninjas** e pesquise por `Naruto`.
2. Abra **Missões → Gerenciar missões** e selecione **Capturar gato perdido**. A equipe de exemplo deve mostrar Naruto na função **Suporte**.
3. Vincule Naruto à missão **Escolta de comerciante**, de rank C.
4. Abra **Relatórios → Consultar views** para conferir a participação salva. Em **Totalizadores**, clique em **Gerar totalizadores** para testar a gravação do relatório.

## Estrutura do projeto

| Caminho | Responsabilidade |
| --- | --- |
| `src/Main.java` | Lê as credenciais e inicia a aplicação. |
| `src/view/` | Telas e componentes da interface Swing. |
| `src/model/` | Classes que representam ninjas, missões, vínculos e totalizadores. |
| `src/service/` | Regras de vínculo entre ninjas e missões. |
| `src/dao/` | Consultas e operações de gravação no MySQL. |
| `src/connection/` | Configuração da conexão JDBC. |
| `sql/` | Scripts de instalação e migração do banco. |
| `lib/` | Driver JDBC do MySQL. |

## Solução de problemas

| Sintoma | O que conferir |
| --- | --- |
| O programa não conecta ao MySQL | Confirme se o servidor está ativo na porta `3306`, se o banco `Trabalho_Ninja` existe e se as credenciais foram definidas na configuração de execução. |
| A IDE não encontra a classe `Main` | Confira se `src/` está configurada como pasta de código-fonte e se o projeto está usando o JDK 21. |
| O programa não encontra o driver JDBC | Confira se o arquivo `.jar` da pasta `lib/` foi adicionado ao classpath. |
| As listas de seleção estão vazias | Confira se executou `02_inserir_dados_iniciais.sql`. |
| A tela **Consultar views** apresenta erro | Confira se executou `03_criar_views.sql` depois dos dois primeiros scripts. |
