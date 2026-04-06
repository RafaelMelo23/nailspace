function initRegister() {
    const registerForm = document.getElementById('register-form');
    const btnRegister = document.getElementById('btn-register');
    const phoneInput = document.getElementById('phone');

    if (phoneInput) {
        phoneInput.addEventListener('input', (e) => {
            let v = e.target.value.replace(/\D/g, '');
            if (v.length > 11) v = v.slice(0, 11);

            if (v.length > 10) {
                e.target.value = `(${v.slice(0, 2)}) ${v.slice(2, 7)}-${v.slice(7)}`;
            } else if (v.length > 5) {
                e.target.value = `(${v.slice(0, 2)}) ${v.slice(2, 6)}-${v.slice(6)}`;
            } else if (v.length > 2) {
                e.target.value = `(${v.slice(0, 2)}) ${v.slice(2)}`;
            } else if (v.length > 0) {
                e.target.value = `(${v}`;
            }
        });
    }

    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const fullName = document.getElementById('fullname').value;
            const email = document.getElementById('email').value;
            const confirmEmail = document.getElementById('confirm-email').value;
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirm-password').value;
            const phone = phoneInput.value.replace(/\D/g, '');

            if (email !== confirmEmail) {
                UI.showToast('Os e-mails não coincidem!', 'error');
                return;
            }

            if (password !== confirmPassword) {
                UI.showToast('As senhas não coincidem!', 'error');
                return;
            }

            if (password.length < 8) {
                UI.showToast('A senha deve ter pelo menos 8 caracteres!', 'error');
                return;
            }

            if (phone.length !== 11) {
                UI.showToast('O telefone deve conter 11 dígitos (DDD + número)!', 'error');
                return;
            }

            UI.setLoading(btnRegister, true, 'Criando conta...');

            try {
                const response = await fetch('/api/v1/auth/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        fullName: fullName,
                        email: email,
                        rawPassword: password,
                        phoneNumber: phone
                    })
                });

                if (response.ok) {
                    UI.showToast('Conta criada com sucesso! Redirecionando para login...', 'success');
                    setTimeout(() => {
                        App.navigate('/entrar');
                    }, 2000);
                }
            } catch (error) {
                console.error('Registration error:', error);
            } finally {
                UI.setLoading(btnRegister, false, 'Cadastrar');
            }
        });
    }
}
