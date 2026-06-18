document.addEventListener("DOMContentLoaded", () => {
    // API Configurations
    const API_BASE_URL = "http://localhost:8000";
    const API_PREFIX = "/api/v1/ai";

    // DOM Elements
    const body = document.documentElement;
    const themeToggleBtn = document.getElementById("themeToggleBtn");
    
    const toggleSidebarBtn = document.getElementById("toggleSidebarBtn");
    const closeSidebarBtn = document.getElementById("closeSidebarBtn");
    const sidebar = document.querySelector(".sidebar");
    
    const statusDot = document.getElementById("statusDot");
    const statusText = document.getElementById("statusText");
    const modelVal = document.getElementById("modelVal");
    const embedVal = document.getElementById("embedVal");
    const collectionVal = document.getElementById("collectionVal");
    
    const topKSlider = document.getElementById("topKSlider");
    const topKValue = document.getElementById("topKValue");
    
    const dataDirInput = document.getElementById("dataDirInput");
    const resetDbCheck = document.getElementById("resetDbCheck");
    const ingestBtn = document.getElementById("ingestBtn");
    
    const chatForm = document.getElementById("chatForm");
    const userInput = document.getElementById("userInput");
    const chatMessages = document.getElementById("chatMessages");
    const newChatBtn = document.getElementById("newChatBtn");
    const sidebarNewChatBtn = document.getElementById("sidebarNewChatBtn");
    
    const toastElement = document.getElementById("toast");
    const welcomeTime = document.getElementById("welcomeTime");

    // Initialize welcome message timestamp
    const now = new Date();
    welcomeTime.textContent = formatTime(now);

    // Track active typing indicator element
    let typingIndicatorElement = null;

    /* ==========================================================================
       1. Theme Toggle & Initialization
       ========================================================================== */
    const savedTheme = localStorage.getItem("theme") || "dark";
    body.setAttribute("data-theme", savedTheme);

    themeToggleBtn.addEventListener("click", () => {
        const currentTheme = body.getAttribute("data-theme");
        const newTheme = currentTheme === "dark" ? "light" : "dark";
        body.setAttribute("data-theme", newTheme);
        localStorage.setItem("theme", newTheme);
        showToast(`Đã chuyển sang chế độ ${newTheme === "dark" ? "Tối" : "Sáng"}`, "info");
    });

    /* ==========================================================================
       2. Responsive Sidebar Controls
       ========================================================================== */
    toggleSidebarBtn.addEventListener("click", () => {
        sidebar.classList.add("active");
    });

    closeSidebarBtn.addEventListener("click", () => {
        sidebar.classList.remove("active");
    });

    // Close sidebar on escape key or clicking outside on mobile
    document.addEventListener("click", (e) => {
        if (window.innerWidth <= 768) {
            if (!sidebar.contains(e.target) && !toggleSidebarBtn.contains(e.target) && sidebar.classList.contains("active")) {
                sidebar.classList.remove("active");
            }
        }
    });

    /* ==========================================================================
       3. Real-time API Server Health Polling
       ========================================================================== */
    async function checkServerHealth() {
        try {
            const response = await fetch(`${API_BASE_URL}${API_PREFIX}/health`);
            if (!response.ok) throw new Error("Server health check failed");
            
            const data = await response.json();
            
            // Update UI with connection success
            statusDot.className = "status-dot status-online";
            statusText.textContent = "Đã kết nối";
            modelVal.textContent = data.model || "qwen3.5:2b";
            embedVal.textContent = data.embedding_model || "nomic-embed-text";
            collectionVal.textContent = data.collection || "farmtrace_knowledge";
        } catch (error) {
            console.error("Health check error:", error);
            // Update UI with connection failure
            statusDot.className = "status-dot status-offline";
            statusText.textContent = "Mất kết nối";
            modelVal.textContent = "-";
            embedVal.textContent = "-";
            collectionVal.textContent = "-";
        }
    }

    // Run health check immediately and then every 10 seconds
    checkServerHealth();
    setInterval(checkServerHealth, 10000);

    /* ==========================================================================
       4. Slider configuration and quick prompts
       ========================================================================== */
    topKSlider.addEventListener("input", (e) => {
        topKValue.textContent = e.target.value;
    });

    // Quick prompt buttons
    document.querySelectorAll(".quick-prompt-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            userInput.value = btn.textContent;
            userInput.focus();
            
            // On mobile devices, close sidebar automatically after clicking a prompt
            if (window.innerWidth <= 768) {
                sidebar.classList.remove("active");
            }
        });
    });

    /* ==========================================================================
       5. Document Ingestion Logic
       ========================================================================== */
    ingestBtn.addEventListener("click", async () => {
        const dataDir = dataDirInput.value.trim() || null;
        const reset = resetDbCheck.checked;
        
        // Disable UI during ingestion
        ingestBtn.disabled = true;
        const btnIcon = ingestBtn.querySelector(".btn-icon");
        const btnText = ingestBtn.querySelector(".btn-text");
        
        btnIcon.classList.add("spinning");
        btnText.textContent = "Đang nạp tài liệu...";
        showToast("Đang tiến hành nạp tài liệu vào Vector DB. Có thể mất một chút thời gian...", "info");

        try {
            const response = await fetch(`${API_BASE_URL}${API_PREFIX}/ingest`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    data_dir: dataDir,
                    reset: reset
                })
            });

            const data = await response.json();
            
            if (response.ok) {
                showToast(`Nạp thành công! Đã load ${data.files_loaded} file, index ${data.chunks_indexed} đoạn dữ liệu.`, "success");
                // Refresh health check to see if database collection changed
                checkServerHealth();
            } else {
                throw new Error(data.detail || "Lỗi không xác định từ server");
            }
        } catch (error) {
            console.error("Ingestion error:", error);
            showToast(`Nạp tài liệu thất bại: ${error.message}`, "error");
        } finally {
            // Restore button UI
            ingestBtn.disabled = false;
            btnIcon.classList.remove("spinning");
            btnText.textContent = "Bắt đầu nạp tài liệu";
        }
    });

    /* ==========================================================================
       6. Chat Logic (Submitting questions, fetching answers, formatting results)
       ========================================================================== */
    chatForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        
        const question = userInput.value.trim();
        if (!question) return;

        // 1. Add user bubble to chat window
        appendMessage(question, "user");
        userInput.value = "";
        
        // 2. Add typing indicator
        showTypingIndicator();
        
        // 3. Send API request
        try {
            const response = await fetch(`${API_BASE_URL}${API_PREFIX}/chat`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    message: question,
                    top_k: parseInt(topKSlider.value)
                })
            });

            const data = await response.json();
            
            // Remove typing indicator
            hideTypingIndicator();

            if (response.ok) {
                // 4. Render AI answer
                appendMessage(data.answer, "ai", data.sources);
            } else {
                throw new Error(data.detail || "Không nhận được phản hồi hợp lệ từ chatbot RAG");
            }
        } catch (error) {
            console.error("Chat request error:", error);
            hideTypingIndicator();
            appendMessage(`❌ **Lỗi:** Không thể lấy câu trả lời từ hệ thống. Chi tiết: *${error.message}*. Hãy đảm bảo server backend đang chạy và Ollama đã sẵn sàng.`, "ai");
        }
    });

    /* ==========================================================================
       7. Utility Helpers (HTML builders, Markdown parses, timestamps, toasts)
       ========================================================================== */
    
    // Add time formatting
    function formatTime(date) {
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    }

    // Escape raw HTML strings to prevent XSS
    function escapeHTML(str) {
        return str
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // Basic Markdown to HTML converter
    function parseMarkdown(text) {
        let html = escapeHTML(text);

        // Bold text: **text** or __text__
        html = html.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");
        html = html.replace(/__(.*?)__/g, "<strong>$1</strong>");

        // Italic text: *text* or _text_
        html = html.replace(/\*(.*?)\*/g, "<em>$1</em>");
        html = html.replace(/_(.*?)_/g, "<em>$1</em>");

        // Code snippets: `code`
        html = html.replace(/`(.*?)`/g, "<code>$1</code>");

        // Split text by double newlines into paragraphs
        const paragraphs = html.split(/\n\n+/);
        let formattedHtml = paragraphs.map(p => {
            p = p.trim();
            if (!p) return "";

            // Bullet lists: Lines starting with "- " or "* "
            if (p.startsWith("- ") || p.startsWith("* ")) {
                const items = p.split(/\n[-*]\s+/);
                // Clean up the first item which still contains the initial marker
                items[0] = items[0].replace(/^[-*]\s+/, "");
                return "<ul>" + items.map(item => `<li>${item.replace(/\n/g, "<br>")}</li>`).join("") + "</ul>";
            }
            
            // Ordered lists: Lines starting with "1. " or "2. "
            if (/^\d+\.\s+/.test(p)) {
                const items = p.split(/\n\d+\.\s+/);
                items[0] = items[0].replace(/^\d+\.\s+/, "");
                return "<ol>" + items.map(item => `<li>${item.replace(/\n/g, "<br>")}</li>`).join("") + "</ol>";
            }

            return `<p>${p.replace(/\n/g, "<br>")}</p>`;
        }).join("");

        return formattedHtml;
    }

    // Create and append messages
    function appendMessage(text, sender, sources = []) {
        const messageDiv = document.createElement("div");
        messageDiv.className = `message message-${sender}`;

        const avatarDiv = document.createElement("div");
        avatarDiv.className = "message-avatar";
        avatarDiv.innerHTML = sender === "user" ? '<i class="fa-solid fa-user"></i>' : '<i class="fa-solid fa-robot"></i>';

        const wrapperDiv = document.createElement("div");
        wrapperDiv.className = "message-content-wrapper";

        const bubbleDiv = document.createElement("div");
        bubbleDiv.className = "message-bubble";
        bubbleDiv.innerHTML = parseMarkdown(text);

        // If it is AI and we have sources, append the sources layout
        if (sender === "ai" && sources && sources.length > 0) {
            const sourcesContainer = document.createElement("div");
            sourcesContainer.className = "sources-container";

            const toggleHeader = document.createElement("div");
            toggleHeader.className = "sources-toggle";
            toggleHeader.innerHTML = `<i class="fa-solid fa-chevron-right"></i> Xem nguồn tài liệu tham khảo (${sources.length})`;
            
            const sourcesList = document.createElement("div");
            sourcesList.className = "sources-list";

            sources.forEach((src, idx) => {
                const sourceCard = document.createElement("div");
                sourceCard.className = "source-card";

                const pageText = src.page !== null && src.page !== undefined ? `<span class="source-page">Trang ${src.page}</span>` : "";
                
                // Get filename only from path
                const filename = src.source.split(/[/\\]/).pop();

                sourceCard.innerHTML = `
                    <div class="source-card-header">
                        <div class="source-title-wrapper">
                            <i class="fa-solid fa-file-lines source-icon"></i>
                            <span class="source-title" title="${src.source}">${filename}</span>
                        </div>
                        ${pageText}
                    </div>
                    <div class="source-snippet">${escapeHTML(src.snippet)}</div>
                `;

                // Expand/collapse snippet on header click
                const header = sourceCard.querySelector(".source-card-header");
                header.addEventListener("click", () => {
                    sourceCard.classList.toggle("expanded");
                });

                sourcesList.appendChild(sourceCard);
            });

            // Expand/collapse sources block
            toggleHeader.addEventListener("click", () => {
                const isActive = toggleHeader.classList.toggle("active");
                sourcesList.classList.toggle("active");
                if (isActive) {
                    toggleHeader.innerHTML = `<i class="fa-solid fa-chevron-down"></i> Thu gọn nguồn tham khảo (${sources.length})`;
                } else {
                    toggleHeader.innerHTML = `<i class="fa-solid fa-chevron-right"></i> Xem nguồn tài liệu tham khảo (${sources.length})`;
                }
                // Scroll down slightly to make sure the newly expanded content is visible
                setTimeout(() => {
                    chatMessages.scrollTop = chatMessages.scrollHeight;
                }, 50);
            });

            sourcesContainer.appendChild(toggleHeader);
            sourcesContainer.appendChild(sourcesList);
            bubbleDiv.appendChild(sourcesContainer);
        }

        const timeSpan = document.createElement("span");
        timeSpan.className = "message-time";
        timeSpan.textContent = formatTime(new Date());

        wrapperDiv.appendChild(bubbleDiv);
        wrapperDiv.appendChild(timeSpan);
        
        messageDiv.appendChild(avatarDiv);
        messageDiv.appendChild(wrapperDiv);

        chatMessages.appendChild(messageDiv);
        
        // Smooth scroll to bottom
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // Show typing status indicator
    function showTypingIndicator() {
        if (typingIndicatorElement) return;

        const messageDiv = document.createElement("div");
        messageDiv.className = "message message-ai typing-container";

        const avatarDiv = document.createElement("div");
        avatarDiv.className = "message-avatar";
        avatarDiv.innerHTML = '<i class="fa-solid fa-robot"></i>';

        const wrapperDiv = document.createElement("div");
        wrapperDiv.className = "message-content-wrapper";

        const bubbleDiv = document.createElement("div");
        bubbleDiv.className = "message-bubble typing-indicator";
        bubbleDiv.innerHTML = `
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
        `;

        wrapperDiv.appendChild(bubbleDiv);
        messageDiv.appendChild(avatarDiv);
        messageDiv.appendChild(wrapperDiv);

        chatMessages.appendChild(messageDiv);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        typingIndicatorElement = messageDiv;
    }

    // Hide typing status indicator
    function hideTypingIndicator() {
        if (typingIndicatorElement) {
            typingIndicatorElement.remove();
            typingIndicatorElement = null;
        }
    }

    // Show custom Toast notification
    let toastTimeout = null;
    function showToast(message, type = "info") {
        // Clear existing timeout
        if (toastTimeout) {
            clearTimeout(toastTimeout);
        }

        // Setup styles based on type
        toastElement.className = `toast toast-${type}`;
        
        let iconHtml = "";
        if (type === "success") iconHtml = '<i class="fa-solid fa-circle-check"></i>';
        else if (type === "error") iconHtml = '<i class="fa-solid fa-triangle-exclamation"></i>';
        else iconHtml = '<i class="fa-solid fa-circle-info"></i>';

        toastElement.innerHTML = `${iconHtml} <span>${message}</span>`;
        toastElement.classList.remove("hidden");

        // Auto hide after 4 seconds
        toastTimeout = setTimeout(() => {
            toastElement.classList.add("hidden");
        }, 4000);
    }

    /* ==========================================================================
       8. New Chat Action Implementation
       ========================================================================== */
    function startNewChat() {
        // Clear chat messages
        chatMessages.innerHTML = "";
        
        // Create new welcome message
        const welcomeMessageHtml = `
            <div class="message message-ai">
                <div class="message-avatar">
                    <i class="fa-solid fa-robot"></i>
                </div>
                <div class="message-content-wrapper">
                    <div class="message-bubble">
                        <p>Xin chào! Tôi là trợ lý AI của FarmTrace. Tôi có thể hỗ trợ bạn tìm kiếm và trả lời các câu hỏi liên quan đến tiêu chuẩn VietGAP hoặc hệ thống FarmTrace.</p>
                        <p>Hãy chọn một câu hỏi gợi ý bên trái hoặc nhập câu hỏi của bạn dưới đây!</p>
                    </div>
                    <span class="message-time">${formatTime(new Date())}</span>
                </div>
            </div>
        `;
        chatMessages.innerHTML = welcomeMessageHtml;
        
        // Show success toast
        showToast("Đã bắt đầu cuộc hội thoại mới", "success");

        // Clear user input and focus
        userInput.value = "";
        userInput.focus();
    }

    if (newChatBtn) {
        newChatBtn.addEventListener("click", startNewChat);
    }
    
    if (sidebarNewChatBtn) {
        sidebarNewChatBtn.addEventListener("click", () => {
            startNewChat();
            // On mobile devices, close sidebar automatically after starting a new chat
            if (window.innerWidth <= 768) {
                sidebar.classList.remove("active");
            }
        });
    }
});
