// nav.js

document.addEventListener('DOMContentLoaded', () => {
    const rawUser = localStorage.getItem('user');
    const user = rawUser ? JSON.parse(rawUser) : null;
    console.log('nav.js loaded, user from storage:', user);
    const navContainer = document.getElementById('navbar-container');
    if (!navContainer) return;

    // Farmers don't need the generic Home link
    const homeLink = (user && user.role === 'FARMER') ? '' : '<li><a href="index.html">Home</a></li>';
    const brandLink = (user && user.role === 'FARMER') ? 'farmer-dashboard.html' : 'index.html';

    // Navbar HTML construction
    let navContent = `
        <nav class="navbar">
            <a href="${brandLink}" class="brand">
                <img src="images/brand.png" alt="Logo" style="height: 48px; width: auto;">
                Farm Direct
            </a>
            
            <ul class="nav-links">
                ${homeLink}
    `;

    if (user) {
        if (user.role === 'FARMER') {
            navContent += `
                <li><a href="farmer-dashboard.html">Dashboard</a></li>
                <li><a href="farmer-profile-edit.html">My Profile</a></li>
                <li><a href="my-products.html">My Products</a></li>
                <li><a href="farmer-orders.html">Orders</a></li>
                <li><a href="chat.html">Messages</a></li>
            `;
        } else {
            navContent += `
                <li><a href="customer-dashboard.html">My Orders</a></li>
                <li><a href="cart.html">Cart</a></li>
                <li><a href="chat.html">Messages</a></li>
            `;
        }
        navContent += `
                <li>Hello, ${user.name}</li>
                <li><a onclick="logout()" style="cursor: pointer;">Logout</a></li>
        `;
    } else {
        navContent += `
            <li><a href="signin.html">Login</a></li>
            <li><a href="register.html" class="btn-primary" style="padding: 0.5rem 1rem; color: white;">Register</a></li>
        `;
    }

    navContent += `
            </ul>
            <!-- Mobile Hamburger -->
            <div class="burger">
                <div class="line1"></div>
                <div class="line2"></div>
                <div class="line3"></div>
            </div>
        </nav>
    `;

    navContainer.innerHTML = navContent;

    // Mobile Menu Interaction
    const burger = document.querySelector('.burger');
    const nav = document.querySelector('.nav-links');
    const navLinks = document.querySelectorAll('.nav-links li');

    if (burger) {
        burger.addEventListener('click', () => {
            // Toggle Nav
            nav.classList.toggle('nav-active');

            // Animate Links
            navLinks.forEach((link, index) => {
                if (link.style.animation) {
                    link.style.animation = '';
                } else {
                    link.style.animation = `navLinkFade 0.5s ease forwards ${index / 7 + 0.3}s`;
                }
            });

            // Burger Animation
            burger.classList.toggle('toggle');
        });
    }
});
