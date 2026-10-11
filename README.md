# GymLog 🏋️

Aplicação Android para registar os treinos de ginásio e acompanhar a evolução, ao estilo do Hevy. É gratuita, não tem anúncios nem pede conta, e os dados ficam no telemóvel.

*Android workout tracker inspired by Hevy: workout logging, PR alerts, progress charts, routines, 6 languages, optional AI coach (Gemini) and Hevy import. Fully offline, no account needed.*

**[⬇️ Descarregar a última versão (APK)](../../releases/latest)** · Android 8.0 ou superior

---

## Funcionalidades

### Treinos
- Treino em curso com peso × repetições, só repetições ou duração. Os tipos de série são normal, aquecimento, drop set e até à falha.
- A coluna **"Anterior"** mostra o que fizeste da última vez. Se marcares uma série sem preencher os valores, a app usa esses.
- **Temporizador de descanso** automático, configurável por exercício (±15 s ou saltar). No fim, vibra e mostra uma notificação.
- **Alertas de recorde pessoal (PR)** 🏆 assim que bates um recorde: peso máximo, 1RM estimado, melhor série em volume, mais repetições ou maior duração.
- Notas por treino e por exercício, ecrã sempre ligado durante o treino, edição de treinos já terminados.

### Rotinas e exercícios
- Rotinas com séries, repetições, peso e descanso, que inicias com um toque. Também podes repetir um treino ou guardá-lo como rotina.
- **122 exercícios incluídos**, com pesquisa e filtro por músculo. Podes criar os teus.
- **Fotos dos exercícios** (posição inicial e final, a alternar) e atalho para vídeos da técnica no YouTube.

### Progresso
- **Gráficos por exercício**: 1RM estimado, peso máximo, melhor série, volume e repetições, em 1M / 3M / 6M / 1A / Tudo.
- Recordes pessoais e "melhor peso por número de repetições".
- Histórico ilimitado, organizado por mês.
- Estatísticas: treinos e volume por semana, séries por músculo, sequência de semanas a treinar.
- Registo de peso corporal com gráfico.

### Ajudas opcionais
Cada uma liga e desliga em **Perfil → Ajudas ao treino**.
- 💡 **Sugestões de progressão**: propõe o próximo peso e repetições a partir das sessões anteriores (progressão dupla e alívio de ~10% quando estagnas).
- 🤖 **Treinador IA (Google Gemini)**: conversa com um treinador que vê o teu histórico e analisa treinos. Usa a tua própria chave gratuita (ver abaixo). **Desligado por defeito.**

### Dados
- **Importação do Hevy** (ficheiro CSV).
- Cópia de segurança completa em JSON (exportar e importar).
- kg ou lb.
- **6 línguas**: 🇵🇹 Português · 🇬🇧 English · 🇪🇸 Español · 🇫🇷 Français · 🇩🇪 Deutsch · 🇮🇹 Italiano. A app segue a língua do sistema ou a que escolheres; os nomes dos exercícios também são traduzidos.

---

## Instalação

1. Descarrega o `GymLog.apk` da [página de Releases](../../releases/latest).
2. Abre-o no telemóvel e toca em **Instalar**. Se aparecer um aviso de "fontes desconhecidas", permite a instalação.
3. As versões novas instalam-se por cima e mantêm os dados.

## Importar do Hevy

1. No Hevy: **Perfil → Definições → Export & Import Data → Export Workouts**. Isto gera um ficheiro `workout_data.csv`.
2. No GymLog: **Perfil → Dados → Importar do Hevy** e escolhe o ficheiro.

O que é importado e o que não é:
- Os exercícios do Hevy são associados aos da app pelo nome. Os que não existirem são criados como personalizados, com o músculo e o equipamento deduzidos do nome.
- Treinos que já existam são ignorados, por isso podes importar o mesmo ficheiro mais do que uma vez.
- A distância de exercícios de cardio, o RPE e as superséries não são importados.

## Treinador IA (opcional e gratuito)

1. Cria uma chave gratuita em **[Google AI Studio](https://aistudio.google.com/apikey)**.
2. No GymLog: **Perfil → Ajudas ao treino → Ativar treinador IA** e cola a chave. Podes usar **"Testar ligação"** para confirmar.
3. A app escolhe automaticamente o modelo *Flash* mais recente disponível; podes mudá-lo à mão.

> **Privacidade:** quando usas o treinador, é enviado à Google um resumo dos teus treinos. No plano gratuito do Gemini, a Google pode usar esses dados para melhorar os seus produtos. A chave fica guardada apenas no telemóvel. Se não ativares o treinador, nenhum dado sai do telemóvel. As fotos dos exercícios são descarregadas da internet, mas não envias nada.

---

## Compilar a partir do código

Requisitos: **JDK 17** e **Android SDK** (platform 34).

```bash
git clone https://github.com/Andureh/GymLog.git
cd GymLog
./gradlew assembleRelease      # APK em app/build/outputs/apk/release/
./gradlew testDebugUnitTest    # testes unitários
```

Também abre diretamente no Android Studio. A versão *release* é assinada com a chave de debug local, para poder ser instalada diretamente.

> Para atualizar uma app já instalada, o APK novo tem de ser assinado com a mesma chave. Um APK compilado noutro computador obriga a desinstalar primeiro, o que apaga os dados. Exporta uma cópia de segurança antes.

### Tecnologias
Kotlin · Jetpack Compose (Material 3) · Room (SQLite) · Navigation Compose · coroutines/Flow. Não usa bibliotecas de terceiros: os gráficos, o carregamento de imagens e o cliente do Gemini são implementados na própria app.

### Estrutura

```
app/src/main/java/com/gymlog/app/
├── GymApp.kt, MainActivity.kt   # arranque, dependências, temporizador de descanso
├── data/        # Room (entidades, DAOs, migrações), repositório, definições,
│                #   cópias de segurança, importação do Hevy, fotos dos exercícios
├── domain/      # recordes (PR, 1RM), sessões, sugestões de progressão
├── ai/          # cliente Gemini, treinador e resumo dos treinos enviado ao modelo
├── i18n/        # textos das 6 línguas + catálogo traduzido de exercícios
├── ui/          # navegação, tema, componentes (gráficos, campos, imagens) e ecrãs
└── util/        # formatação de números/datas/unidades, vibração
app/src/test/    # testes unitários (recordes, traduções, progressão, importação)
app/schemas/     # esquema da base de dados por versão (migrações)
```

### Traduções
Cada língua é um ficheiro em [`i18n/`](app/src/main/java/com/gymlog/app/i18n). Se faltar algum texto numa língua, o projeto não compila. Os nomes dos exercícios, músculos e equipamento estão em [`Catalog.kt`](app/src/main/java/com/gymlog/app/i18n/Catalog.kt), uma linha por exercício com as 6 línguas.

---

## Créditos

- Fotos dos exercícios: [Free Exercise DB](https://github.com/yuhonas/free-exercise-db) (domínio público / Unlicense).
- Inspirado no [Hevy](https://www.hevyapp.com/). Este projeto não tem qualquer ligação ao Hevy.

## Licença

[MIT](LICENSE)
