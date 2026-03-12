// auth.js

console.log('auth.js loaded');

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOMContentLoaded fired in auth.js');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    // Read optional page-specific role requirement, e.g. FARMER or CUSTOMER
    const body = document.body;
    const pageLoginRoleAttr = body ? (body.getAttribute('data-login-role') || body.dataset.loginRole) : null;
    const requiredLoginRole = pageLoginRoleAttr ? String(pageLoginRoleAttr).toUpperCase() : null;

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            console.log('Login form submit event fired');
            e.preventDefault();
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            console.log('Attempting login with email:', email);

            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                console.log('Response status:', response.status);
                console.log('Response ok:', response.ok);

                if (response.ok) {
                    const user = await response.json();
                    console.log('Login successful, user data:', user);
                    // ensure role is a string and normalized
                    if (user && user.role) {
                        user.role = String(user.role).toUpperCase();
                    }

                    // If this page is restricted to a specific role,
                    // block login when the role does not match.
                    if (requiredLoginRole && user && user.role !== requiredLoginRole) {
                        console.warn('Role mismatch for page, expected=', requiredLoginRole, 'actual=', user.role);
                        alert(requiredLoginRole === 'FARMER'
                            ? 'This page is only for Farmer login. Please use Customer Login for customer accounts.'
                            : 'This page is only for Customer login. Please use Farmer Login for farmer accounts.');
                        localStorage.removeItem('user');
                        return;
                    }

                    localStorage.setItem('user', JSON.stringify(user));
                    console.log('User stored in localStorage');
                    alert('Login Successful!');

                    if (user.role === 'FARMER') {
                        console.log('Redirecting to farmer-dashboard');
                        window.location.href = '/farmer-dashboard.html';
                    } else {
                        console.log('Redirecting to index');
                        window.location.href = '/index.html';
                    }
                } else {
                    console.log('Login failed with status', response.status);
                    // More specific, role-aware error message for wrong email/password
                    if (requiredLoginRole === 'FARMER') {
                        alert('Invalid email or password for Farmer account. Please enter the correct password.');
                    } else if (requiredLoginRole === 'CUSTOMER') {
                        alert('Invalid email or password for Customer account. Please enter the correct password.');
                    } else {
                        alert('Invalid email or password. Please enter the correct password.');
                    }
                }
            } catch (error) {
                console.error('Login error:', error);
                alert('Login failed');
            }
        });
    }

    if (registerForm) {
        const roleSelect = document.getElementById('role');
        const farmerFields = document.getElementById('farmerFields');
        const syncRoleFields = () => {
            if (!roleSelect || !farmerFields) return;
            farmerFields.style.display = roleSelect.value === 'FARMER' ? 'block' : 'none';
        };
        if (roleSelect) {
            roleSelect.addEventListener('change', syncRoleFields);
            syncRoleFields();
        }

        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('name').value;
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const role = document.getElementById('role').value;
            const farmName = document.getElementById('farmName') ? document.getElementById('farmName').value : '';
            const state = document.getElementById('state') ? document.getElementById('state').value : '';
            const district = document.getElementById('district') ? document.getElementById('district').value : '';
            const pickupAddress = document.getElementById('pickupAddress') ? document.getElementById('pickupAddress').value : '';

            if (role === 'FARMER' && (!pickupAddress || !pickupAddress.trim())) {
                alert('Pickup Address is required for Farmer registration.');
                return;
            }

            try {
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, email, password, role, farmName, state, district, pickupAddress })
                });

                if (response.ok) {
                    alert('Registration Successful! Please login.');
                    window.location.href = 'signin.html';
                } else {
                    alert('Registration failed');
                }
            } catch (error) {
                console.error(error);
                alert('Error registering');
            }
        });
    }
});

// Logout utility
function logout() {
    localStorage.removeItem('user');
    window.location.href = 'signin.html';
}
