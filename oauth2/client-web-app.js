require('dotenv').config(); // Carrega .env
const express = require("express");
const cookieParser = require("cookie-parser");
const axios = require("axios");
const FormData = require("form-data");
const jwt = require("jsonwebtoken");

const app = express();
const PORT = process.env.PORT || 3001;

// Configuração
const CLIENT_ID = process.env.CLIENT_ID;
const CLIENT_SECRET = process.env.CLIENT_SECRET;
const SECRET_KEY = process.env.SECRET_KEY_COOKIE;
const CALLBACK_URL = `http://localhost:${PORT}/callback`;

// Scopes: OpenID, Email e Google Tasks (Leitura)
const SCOPES = "openid email https://www.googleapis.com/auth/tasks.readonly";

app.use(cookieParser(SECRET_KEY));
app.use(express.urlencoded({ extended: true })); // Para ler body de forms POST

// Rota Inicial
app.get("/", (req, resp) => {
    if (req.signedCookies.access_token) {
        resp.send(`
            <h1>Bem-vindo!</h1>
            <p>Você já está logado.</p>
            <form action="/tasklist" method="POST">
                <label>ID da Lista de Tarefas:</label>
                <input type="text" name="tasklist_id" placeholder="Ex: @default ou ID" required>
                <button type="submit">Ver Tarefas</button>
            </form>
            <br><a href="/logout">Logout</a>
        `);
    } else {
        resp.send("<a href='/login'>Login com Google</a>");
    }
});

// 1. Redirecionar para Google Login
app.get("/login", (req, resp) => {
    // State DEVE ser aleatório e validado no callback para evitar CSRF
    const crypto = require('crypto');
    const state = crypto.randomBytes(16).toString('hex');

    // Guardar state em cookie assinado (tempo de vida curto, ex: 5 min)
    resp.cookie("auth_state", state, { httpOnly: true, signed: true, maxAge: 300000 });

    const authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
        `client_id=${CLIENT_ID}&` +
        `response_type=code&` +
        `redirect_uri=${encodeURIComponent(CALLBACK_URL)}&` +
        `scope=${encodeURIComponent(SCOPES)}&` +
        `state=${state}&` +
        `access_type=offline`; // Para receber refresh token (opcional)

    resp.redirect(authUrl);
});

// 2. Callback - Trocar Code por Token
app.get("/callback", async (req, resp) => {
    const code = req.query.code;
    const state = req.query.state;
    const storedState = req.signedCookies.auth_state;

    // Validar state
    if (!state || !storedState || state !== storedState) {
        console.error("State mismatch or missing!", { state, storedState });
        return resp.status(400).send("Erro de segurança: State inválido (Possível ataque CSRF).");
    }

    // Limpar cookie de state após uso
    resp.clearCookie("auth_state");

    try {
        // Trocar code por tokens
        const response = await axios.post("https://oauth2.googleapis.com/token", {
            code: code,
            client_id: CLIENT_ID,
            client_secret: CLIENT_SECRET,
            redirect_uri: CALLBACK_URL,
            grant_type: "authorization_code"
        });

        const { access_token, id_token } = response.data;

        // Decodificar ID Token para ver email (sem verificar assinatura aqui para simplicidade)
        const userInfo = jwt.decode(id_token);
        console.log("User Info:", userInfo);

        // Guardar Tokens em Cookie Seguro
        const cookieOptions = { httpOnly: true, signed: true };
        resp.cookie("access_token", access_token, cookieOptions);
        resp.cookie("user_email", userInfo.email, cookieOptions);

        // Mostrar Formulário para pedir ID da Task List
        resp.send(`
            <h1>Login Sucesso!</h1>
            <p>Olá, <b>${userInfo.email}</b></p>
            <p>Access Token recebido.</p>
            <hr>
            <h3>Ver Tarefas</h3>
            <form action="/tasklist" method="POST">
                <label>ID da Lista (use '@default' para a principal):</label><br>
                <input type="text" name="tasklist_id" value="@default"><br><br>
                <button type="submit">Listar Tarefas</button>
            </form>
        `);
    } catch (error) {
        console.error("Erro ao obter token:", error.response ? error.response.data : error.message);
        resp.status(500).send("Erro na autenticação");
    }
});

// 3. Rota para Listar Tarefas
app.post("/tasklist", async (req, resp) => {
    const accessToken = req.signedCookies.access_token;

    if (!accessToken) {
        return resp.status(401).send("Não autenticado. <a href='/login'>Faça login</a>");
    }

    const taskListId = req.body.tasklist_id || "@default";

    try {
        const tasksResponse = await axios.get(
            `https://tasks.googleapis.com/tasks/v1/lists/${taskListId}/tasks`,
            {
                headers: {
                    Authorization: `Bearer ${accessToken}`
                },
                params: {
                    showCompleted: true,
                    showHidden: true
                }
            }
        );

        const tasks = tasksResponse.data.items || [];

        let html = `<h1>Tarefas da Lista: ${taskListId}</h1><ul>`;
        tasks.forEach(t => {
            html += `<li>[${t.status}] <b>${t.title}</b></li>`;
        });
        html += "</ul><br><a href='/'>Voltar</a>";

        resp.send(html);

    } catch (error) {
        console.error("Erro na API Tasks:", error.response ? error.response.data : error.message);
        resp.send(`Erro ao ler tarefas: ${error.message} <br> <a href='/'>Voltar</a>`);
    }
});

app.get("/logout", (req, resp) => {
    resp.clearCookie("access_token");
    resp.clearCookie("user_email");
    resp.redirect("/");
});

app.listen(PORT, () => console.log(`Servidor a correr em http://localhost:${PORT}`));
