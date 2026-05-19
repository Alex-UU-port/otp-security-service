let currentToken = null;
let currentRole = null;

document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();
        if (response.ok) {
            currentToken = data.token;
            currentRole = data.role;
            localStorage.setItem('token', currentToken);
            localStorage.setItem('role', currentRole);
            localStorage.setItem('username', data.username);
            showUserPanel();
        } else {
            showResult('loginResult', data.error, false);
        }
    } catch (error) {
        console.error('Ошибка:', error);
    }
});

document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('regUsername').value;
    const password = document.getElementById('regPassword').value;
    const email = document.getElementById('regEmail').value;
    const phone = document.getElementById('regPhone').value;

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email, phone })
        });

        const data = await response.json();
        if (response.ok) {
            alert('Регистрация успешна! Теперь войдите в систему.');
            showLogin();
        } else {
            alert('Ошибка: ' + data.error);
        }
    } catch (error) {
        console.error('Ошибка:', error);
    }
});

async function generateOtp() {
    const channel = document.getElementById('channel').value;
    const destination = document.getElementById('destination').value;

    if (!destination) {
        alert('Введите получателя');
        return;
    }

    try {
        const response = await fetch('/api/user/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + currentToken
            },
            body: JSON.stringify({ channel, destination })
        });

        const data = await response.json();
        const resultDiv = document.getElementById('generateResult');

        if (response.ok) {
            resultDiv.innerHTML = `<div class="result success">✅ ${data.message}<br>ID операции: ${data.operationId}<br>${data.expiresIn}</div>`;
            document.getElementById('operationId').value = data.operationId;
        } else {
            resultDiv.innerHTML = `<div class="result error">❌ Ошибка: ${data.error}</div>`;
        }
    } catch (error) {
        console.error('Ошибка:', error);
    }
}

async function validateOtp() {
    const operationId = document.getElementById('operationId').value;
    const code = document.getElementById('otpCode').value;

    if (!operationId || !code) {
        alert('Введите ID операции и код');
        return;
    }

    try {
        const response = await fetch('/api/user/validate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + currentToken
            },
            body: JSON.stringify({ operationId, code })
        });

        const data = await response.json();
        const resultDiv = document.getElementById('validateResult');

        if (data.success) {
            resultDiv.innerHTML = `<div class="result success">✅ ${data.message}</div>`;
        } else {
            resultDiv.innerHTML = `<div class="result error">❌ ${data.message}</div>`;
        }
    } catch (error) {
        console.error('Ошибка:', error);
    }
}

async function updateConfig() {
    const codeLength = document.getElementById('codeLength').value;
    const ttlSeconds = document.getElementById('ttlSeconds').value;

    try {
        const response = await fetch('/api/admin/config', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + currentToken
            },
            body: JSON.stringify({ codeLength: parseInt(codeLength), ttlSeconds: parseInt(ttlSeconds) })
        });

        const data = await response.json();
        alert(data.message);
    } catch (error) {
        console.error('Ошибка:', error);
    }
}

async function loadUsers() {
    try {
        const response = await fetch('/api/admin/users', {
            headers: { 'Authorization': 'Bearer ' + currentToken }
        });

        const users = await response.json();
        const usersList = document.getElementById('usersList');
        usersList.innerHTML = '<h4>Список пользователей:</h4>';

        users.forEach(user => {
            usersList.innerHTML += `
                <div class="user-item">
                    <span><strong>${user.username}</strong> (ID: ${user.id})</span>
                    <button onclick="deleteUser(${user.id})">Удалить</button>
                </div>
            `;
        });
    } catch (error) {
        console.error('Ошибка:', error);
    }
}

async function deleteUser(userId) {
    if (confirm('Удалить пользователя?')) {
        try {
            const response = await fetch(`/api/admin/users/${userId}`, {
                method: 'DELETE',
                headers: { 'Authorization': 'Bearer ' + currentToken }
            });

            if (response.ok) {
                alert('Пользователь удален');
                loadUsers();
            }
        } catch (error) {
            console.error('Ошибка:', error);
        }
    }
}

function showRegister() {
    document.getElementById('registerCard').style.display = 'block';
    document.querySelector('#authSection .card:first-child').style.display = 'none';
}

function showLogin() {
    document.getElementById('registerCard').style.display = 'none';
    document.querySelector('#authSection .card:first-child').style.display = 'block';
}

function showUserPanel() {
    document.getElementById('authSection').style.display = 'none';
    document.getElementById('userPanel').style.display = 'block';
    document.getElementById('usernameDisplay').textContent = localStorage.getItem('username');

    if (currentRole === 'ADMIN') {
        document.getElementById('adminPanel').style.display = 'block';
    }
}

function logout() {
    localStorage.clear();
    currentToken = null;
    currentRole = null;
    document.getElementById('authSection').style.display = 'block';
    document.getElementById('userPanel').style.display = 'none';
    document.getElementById('loginUsername').value = '';
    document.getElementById('loginPassword').value = '';
}

// Проверяем сохраненную сессию
const savedToken = localStorage.getItem('token');
if (savedToken) {
    currentToken = savedToken;
    currentRole = localStorage.getItem('role');
    showUserPanel();
}