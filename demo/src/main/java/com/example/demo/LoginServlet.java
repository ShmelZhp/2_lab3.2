package com.example.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    /** Кладёт логин в сессию. Id сессии меняем, чтобы его нельзя было подсунуть заранее. */
    static void signIn(HttpServletRequest req, String login) {
        req.getSession(true);
        req.changeSessionId();
        req.getSession().setAttribute("user", login);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("user") != null) {
            resp.sendRedirect(req.getContextPath() + "/files");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String login = UserStore.normalize(req.getParameter("login"));
        String password = req.getParameter("password");

        if (UserStore.get().authenticate(login, password)) {
            signIn(req, login);
            resp.sendRedirect(req.getContextPath() + "/files");
        } else {
            req.setAttribute("error", "Неверный логин или пароль");
            req.setAttribute("login", login);
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        }
    }
}