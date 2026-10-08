"use strict";

// URLs relativas apontam para o gateway que também serve esta página.
const API_BASE = "/api/v1";
const TOKEN_KEY = "sd-commerce-token";

const state = {
    token: sessionStorage.getItem(TOKEN_KEY),
    produtos: [],
    pedidos: [],
    quantidades: new Map()
};

const elements = {
    loginView: document.querySelector("#login-view"),
    appView: document.querySelector("#app-view"),
    loginForm: document.querySelector("#login-form"),
    loginButton: document.querySelector("#login-button"),
    loginError: document.querySelector("#login-error"),
    productsView: document.querySelector("#products-view"),
    ordersView: document.querySelector("#orders-view"),
    productsGrid: document.querySelector("#products-grid"),
    ordersList: document.querySelector("#orders-list"),
    orderSummary: document.querySelector("#order-summary"),
    selectedCount: document.querySelector("#selected-count"),
    estimatedTotal: document.querySelector("#estimated-total"),
    submitOrder: document.querySelector("#submit-order"),
    productsCount: document.querySelector("#products-count"),
    stockCount: document.querySelector("#stock-count"),
    ordersCount: document.querySelector("#orders-count"),
    ordersBadge: document.querySelector("#orders-badge"),
    dialog: document.querySelector("#details-dialog"),
    dialogContent: document.querySelector("#dialog-content"),
    toast: document.querySelector("#toast")
};

const productIcons = {
    notebook: "💻",
    teclado: "⌨️",
    mouse: "🖱️"
};

let toastTimer;

document.addEventListener("DOMContentLoaded", iniciar);

function iniciar() {
    elements.loginForm.addEventListener("submit", autenticar);
    document.querySelector("#logout-button").addEventListener("click", () => encerrarSessao());
    document.querySelector("#refresh-products").addEventListener("click", carregarProdutos);
    document.querySelector("#refresh-orders").addEventListener("click", carregarPedidos);
    elements.submitOrder.addEventListener("click", criarPedido);
    elements.productsGrid.addEventListener("click", tratarCliqueProduto);
    elements.productsGrid.addEventListener("change", tratarQuantidadeDigitada);
    elements.ordersList.addEventListener("click", tratarCliquePedido);
    document.querySelector(".dialog-close").addEventListener("click", () => elements.dialog.close());
    elements.dialog.addEventListener("click", fecharDialogPeloFundo);

    document.querySelectorAll("[data-view]").forEach(button => {
        button.addEventListener("click", () => mostrarSecao(button.dataset.view));
    });

    if (state.token) {
        mostrarAplicacao();
        carregarDashboard();
    } else {
        mostrarLogin();
    }
}

async function autenticar(event) {
    event.preventDefault();
    const form = new FormData(elements.loginForm);
    elements.loginError.hidden = true;
    definirCarregando(elements.loginButton, true, "Entrando…");

    try {
        const resposta = await api("/auth/login", {
            method: "POST",
            body: JSON.stringify({
                usuario: form.get("usuario"),
                senha: form.get("senha")
            }),
            autenticado: false
        });

        state.token = resposta.accessToken;
        // O token permanece nesta aba; a senha não é armazenada pelo frontend.
        sessionStorage.setItem(TOKEN_KEY, state.token);
        mostrarAplicacao();
        await carregarDashboard();
        notificar("Login realizado. Token JWT pronto para as chamadas.", "success");
    } catch (error) {
        elements.loginError.textContent = error.message;
        elements.loginError.hidden = false;
    } finally {
        definirCarregando(elements.loginButton, false, "Entrar no laboratório");
    }
}

async function carregarDashboard() {
    await Promise.allSettled([carregarProdutos(), carregarPedidos()]);
}

async function carregarProdutos() {
    elements.productsGrid.innerHTML = '<div class="loading-card">Consultando o estoque-service via gRPC…</div>';
    try {
        state.produtos = await api("/produtos");
        ajustarQuantidadesAoEstoque();
        renderizarProdutos();
        renderizarResumo();
        renderizarMetricas();
    } catch (error) {
        elements.productsGrid.innerHTML = estadoDeErro(error.message);
        notificar(error.message, "error");
    }
}

async function carregarPedidos() {
    elements.ordersList.innerHTML = '<div class="loading-card">Consultando pedidos persistidos…</div>';
    try {
        state.pedidos = await api("/pedidos");
        renderizarPedidos();
        renderizarMetricas();
    } catch (error) {
        elements.ordersList.innerHTML = estadoDeErro(error.message);
        notificar(error.message, "error");
    }
}

function renderizarProdutos() {
    if (state.produtos.length === 0) {
        elements.productsGrid.innerHTML = estadoVazio("Nenhum produto disponível no catálogo.");
        return;
    }

    elements.productsGrid.innerHTML = state.produtos.map(produto => {
        const quantidade = state.quantidades.get(produto.id) || 0;
        const semEstoque = produto.quantidadeDisponivel === 0;
        const estoqueClass = semEstoque ? "empty" : produto.quantidadeDisponivel <= 5 ? "low" : "";
        const estoqueTexto = semEstoque ? "sem estoque" : `${produto.quantidadeDisponivel} em estoque`;

        return `
            <article class="product-card">
                <div class="product-visual" aria-hidden="true">${productIcons[produto.id] || "📦"}</div>
                <div class="product-body">
                    <div class="product-topline">
                        <h3>${escapar(produto.nome)}</h3>
                        <span class="stock-pill ${estoqueClass}">${estoqueTexto}</span>
                    </div>
                    <p class="product-description">${escapar(produto.descricao)}</p>
                    <div class="product-bottom">
                        <div class="product-price">
                            <strong>${formatarMoeda(produto.preco, produto.moeda)}</strong>
                            <button class="text-button" type="button" data-details="${escapar(produto.id)}">
                                Ver no serviço
                            </button>
                        </div>
                        <div class="quantity-control" aria-label="Quantidade de ${escapar(produto.nome)}">
                            <button type="button" data-change="-1" data-product="${escapar(produto.id)}"
                                    aria-label="Diminuir quantidade" ${quantidade === 0 ? "disabled" : ""}>−</button>
                            <input type="number" min="0" max="${produto.quantidadeDisponivel}"
                                   value="${quantidade}" data-quantity="${escapar(produto.id)}"
                                   aria-label="Quantidade selecionada">
                            <button type="button" data-change="1" data-product="${escapar(produto.id)}"
                                    aria-label="Aumentar quantidade"
                                    ${quantidade >= produto.quantidadeDisponivel ? "disabled" : ""}>+</button>
                        </div>
                    </div>
                </div>
            </article>`;
    }).join("");
}

function renderizarResumo() {
    const selecionados = produtosSelecionados();
    const unidades = selecionados.reduce((total, item) => total + item.quantidade, 0);
    const total = selecionados.reduce((soma, item) => soma + Number(item.produto.preco) * item.quantidade, 0);

    elements.selectedCount.textContent = `${unidades} ${unidades === 1 ? "item" : "itens"}`;
    elements.estimatedTotal.textContent = formatarMoeda(total, selecionados[0]?.produto.moeda || "BRL");
    elements.submitOrder.disabled = selecionados.length === 0;

    if (selecionados.length === 0) {
        elements.orderSummary.innerHTML = `
            <div class="empty-state compact">
                <span aria-hidden="true">＋</span>
                <p>Selecione a quantidade nos produtos ao lado.</p>
            </div>`;
        return;
    }

    elements.orderSummary.innerHTML = selecionados.map(({produto, quantidade}) => `
        <div class="summary-item">
            <strong>${escapar(produto.nome)}</strong>
            <span class="summary-item-price">${formatarMoeda(Number(produto.preco) * quantidade, produto.moeda)}</span>
            <span>${quantidade} × ${formatarMoeda(produto.preco, produto.moeda)}</span>
        </div>`).join("");
}

function renderizarPedidos() {
    if (state.pedidos.length === 0) {
        elements.ordersList.innerHTML = estadoVazio("Nenhum pedido confirmado para este usuário.");
        return;
    }

    elements.ordersList.innerHTML = state.pedidos.map(pedido => `
        <article class="order-card">
            <div class="order-id">
                <strong>Pedido #${escapar(pedido.id.slice(0, 8))}</strong>
                <small>${formatarData(pedido.criadoEm)}</small>
            </div>
            <div class="order-items">
                <span class="status-confirmed">${escapar(pedido.status)}</span>
                <small>${pedido.itens.length} ${pedido.itens.length === 1 ? "produto" : "produtos"}</small>
            </div>
            <strong class="order-value">${formatarMoeda(pedido.valorTotal, pedido.moeda)}</strong>
            <button class="button button-secondary button-small" type="button" data-order="${escapar(pedido.id)}">
                Ver detalhes
            </button>
        </article>`).join("");
}

function renderizarMetricas() {
    elements.productsCount.textContent = state.produtos.length || "0";
    elements.stockCount.textContent = state.produtos.reduce(
        (total, produto) => total + produto.quantidadeDisponivel,
        0
    );
    elements.ordersCount.textContent = state.pedidos.length;
    elements.ordersBadge.textContent = state.pedidos.length;
}

function tratarCliqueProduto(event) {
    const changeButton = event.target.closest("[data-change]");
    if (changeButton) {
        alterarQuantidade(changeButton.dataset.product, Number(changeButton.dataset.change));
        return;
    }

    const detailsButton = event.target.closest("[data-details]");
    if (detailsButton) {
        abrirProduto(detailsButton.dataset.details);
    }
}

function tratarQuantidadeDigitada(event) {
    const input = event.target.closest("[data-quantity]");
    if (!input) return;

    const produto = state.produtos.find(item => item.id === input.dataset.quantity);
    const quantidade = limitar(Number.parseInt(input.value, 10) || 0, 0, produto.quantidadeDisponivel);
    state.quantidades.set(produto.id, quantidade);
    renderizarProdutos();
    renderizarResumo();
}

function alterarQuantidade(produtoId, diferenca) {
    const produto = state.produtos.find(item => item.id === produtoId);
    const atual = state.quantidades.get(produtoId) || 0;
    state.quantidades.set(produtoId, limitar(atual + diferenca, 0, produto.quantidadeDisponivel));
    renderizarProdutos();
    renderizarResumo();
}

async function criarPedido() {
    const itens = produtosSelecionados().map(({produto, quantidade}) => ({
        produtoId: produto.id,
        quantidade
    }));
    if (itens.length === 0) return;

    definirCarregando(elements.submitOrder, true, "Reservando estoque…");
    try {
        const pedido = await api("/pedidos", {
            method: "POST",
            body: JSON.stringify({itens})
        });
        state.quantidades.clear();
        renderizarResumo();
        notificar(`Pedido #${pedido.id.slice(0, 8)} confirmado e estoque reservado.`, "success");
        mostrarPedido(pedido);
        await Promise.allSettled([carregarProdutos(), carregarPedidos()]);
    } catch (error) {
        notificar(error.message, "error");
    } finally {
        definirCarregando(elements.submitOrder, false, "Confirmar pedido");
        renderizarResumo();
    }
}

async function abrirProduto(produtoId) {
    try {
        const produto = await api(`/produtos/${encodeURIComponent(produtoId)}`);
        elements.dialogContent.innerHTML = `
            <div class="dialog-body">
                <p class="eyebrow">Consulta direta ao estoque-service</p>
                <h2>${productIcons[produto.id] || "📦"} ${escapar(produto.nome)}</h2>
                <p>${escapar(produto.descricao)}</p>
                <div class="dialog-meta">
                    <div><span>Identificador</span><strong>${escapar(produto.id)}</strong></div>
                    <div><span>Moeda</span><strong>${escapar(produto.moeda)}</strong></div>
                    <div><span>Preço oficial</span><strong>${formatarMoeda(produto.preco, produto.moeda)}</strong></div>
                    <div><span>Estoque atual</span><strong>${produto.quantidadeDisponivel} unidades</strong></div>
                </div>
            </div>`;
        abrirDialog();
    } catch (error) {
        notificar(error.message, "error");
    }
}

function tratarCliquePedido(event) {
    const button = event.target.closest("[data-order]");
    if (button) abrirPedido(button.dataset.order);
}

async function abrirPedido(pedidoId) {
    try {
        const pedido = await api(`/pedidos/${encodeURIComponent(pedidoId)}`);
        mostrarPedido(pedido);
    } catch (error) {
        notificar(error.message, "error");
    }
}

function mostrarPedido(pedido) {
    elements.dialogContent.innerHTML = `
        <div class="dialog-body">
            <p class="eyebrow">Pedido criado e estoque reservado</p>
            <h2>Pedido #${escapar(pedido.id.slice(0, 8))}</h2>
            <p>${formatarData(pedido.criadoEm)} · <span class="status-confirmed">${escapar(pedido.status)}</span></p>
            <div class="dialog-lines">
                ${pedido.itens.map(item => `
                    <div class="dialog-line">
                        <span><strong>${escapar(item.nome)}</strong><br>${item.quantidade} × ${formatarMoeda(item.precoUnitario, pedido.moeda)}</span>
                        <strong>${formatarMoeda(item.subtotal, pedido.moeda)}</strong>
                    </div>`).join("")}
            </div>
            <div class="dialog-total">
                <span>Total confirmado</span>
                <strong>${formatarMoeda(pedido.valorTotal, pedido.moeda)}</strong>
            </div>
        </div>`;
    abrirDialog();
}

function mostrarSecao(view) {
    const produtosAtivos = view === "produtos";
    elements.productsView.hidden = !produtosAtivos;
    elements.ordersView.hidden = produtosAtivos;
    document.querySelectorAll("[data-view]").forEach(button => {
        button.classList.toggle("active", button.dataset.view === view);
    });
    if (!produtosAtivos) carregarPedidos();
}

function produtosSelecionados() {
    return state.produtos
        .map(produto => ({produto, quantidade: state.quantidades.get(produto.id) || 0}))
        .filter(item => item.quantidade > 0);
}

function ajustarQuantidadesAoEstoque() {
    state.produtos.forEach(produto => {
        const atual = state.quantidades.get(produto.id) || 0;
        state.quantidades.set(produto.id, limitar(atual, 0, produto.quantidadeDisponivel));
    });
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body) headers.set("Content-Type", "application/json");
    if (options.autenticado !== false && state.token) {
        // O gateway autentica cada chamada antes de encaminhá-la por gRPC.
        headers.set("Authorization", `Bearer ${state.token}`);
    }

    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, {...options, headers});
    } catch (error) {
        throw new Error("Não foi possível acessar o API Gateway. Verifique se ele está em execução.");
    }

    const contentType = response.headers.get("content-type") || "";
    const data = contentType.includes("application/json") ? await response.json() : null;

    if (!response.ok) {
        if (response.status === 401 && options.autenticado !== false) {
            encerrarSessao("Sua sessão expirou. Entre novamente.");
        }
        const fieldMessage = data?.fieldErrors ? Object.values(data.fieldErrors)[0] : null;
        throw new Error(fieldMessage || data?.message || `Falha HTTP ${response.status}.`);
    }

    return data;
}

function mostrarAplicacao() {
    elements.loginView.hidden = true;
    elements.appView.hidden = false;
    mostrarSecao("produtos");
}

function mostrarLogin() {
    elements.appView.hidden = true;
    elements.loginView.hidden = false;
    document.querySelector("#usuario").focus();
}

function encerrarSessao(mensagem) {
    sessionStorage.removeItem(TOKEN_KEY);
    state.token = null;
    state.produtos = [];
    state.pedidos = [];
    state.quantidades.clear();
    if (elements.dialog.open) elements.dialog.close();
    mostrarLogin();
    if (mensagem) {
        elements.loginError.textContent = mensagem;
        elements.loginError.hidden = false;
    }
}

function abrirDialog() {
    if (elements.dialog.open) elements.dialog.close();
    elements.dialog.showModal();
}

function fecharDialogPeloFundo(event) {
    if (event.target === elements.dialog) elements.dialog.close();
}

function definirCarregando(button, loading, label) {
    button.disabled = loading;
    button.textContent = label;
}

function notificar(message, type = "") {
    clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.className = `toast ${type}`.trim();
    elements.toast.hidden = false;
    toastTimer = setTimeout(() => {
        elements.toast.hidden = true;
    }, 4500);
}

function formatarMoeda(value, moeda = "BRL") {
    return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: moeda
    }).format(Number(value));
}

function formatarData(value) {
    return new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short"
    }).format(new Date(value));
}

function limitar(value, min, max) {
    return Math.min(Math.max(value, min), max);
}

function escapar(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function estadoVazio(message) {
    return `<div class="empty-state"><span aria-hidden="true">◇</span><p>${escapar(message)}</p></div>`;
}

function estadoDeErro(message) {
    return `<div class="empty-state"><span aria-hidden="true">!</span><p>${escapar(message)}</p></div>`;
}
