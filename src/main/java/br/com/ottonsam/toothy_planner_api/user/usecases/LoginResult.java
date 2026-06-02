package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.AuthTokens;

public record LoginResult(AuthTokens tokens, String message) {

    public boolean authenticated() {
        return tokens != null;
    }
}
