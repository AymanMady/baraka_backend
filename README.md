# 🥙 Baraka Backend

Backend API pour **Baraka**, une application anti-gaspillage alimentaire permettant aux commerçants de vendre leurs invendus à prix réduit.

## 📋 Table des matières

- [Technologies](#-technologies)
- [Prérequis](#-prérequis)
- [Démarrage rapide](#-démarrage-rapide)
- [Configuration](#-configuration)
- [Endpoints API](#-endpoints-api)
- [Base de données](#-base-de-données)
- [Swagger / OpenAPI](#-swagger--openapi)
- [Docker](#-docker)
- [Tests](#-tests)
- [Structure du projet](#-structure-du-projet)

---

## 🛠 Technologies

| Technologie | Version | Description |
|-------------|---------|-------------|
| Java | 17 | Langage de programmation |
| Spring Boot | 3.2.2 | Framework applicatif |
| Spring Security | 6.x | Sécurité & JWT |
| Spring Data JPA | 3.x | Accès aux données |
| PostgreSQL | 16 | Base de données relationnelle |
| Flyway | 10.x | Migrations de base de données |
| MapStruct | 1.5.5 | Mapping DTO ↔ Entity |
| Lombok | - | Réduction du boilerplate |
| SpringDoc OpenAPI | 2.3.0 | Documentation Swagger UI |

---

## 📦 Prérequis

- **Java 17+** (JDK)
- **Maven 3.8+** ou utiliser le wrapper `./mvnw`
- **PostgreSQL 14+** (ou Docker)
- **Docker & Docker Compose** (optionnel, recommandé)

---

## 🚀 Démarrage rapide

### Option 1 : Avec Docker (Recommandé)

```bash
# 1. Cloner le projet
git clone <repository-url>
cd baraka_backend

# 2. Copier et configurer les variables d'environnement
cp .env.example .env
# Éditer .env avec vos valeurs (surtout JWT_SECRET et POSTGRES_PASSWORD)

# 3. Lancer l'application
docker-compose up -d

# 4. Vérifier les logs
docker-compose logs -f app

# 5. Accéder à Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Option 2 : Sans Docker (Développement local)

```bash
# 1. Démarrer PostgreSQL localement
# Créer une base de données: baraka_dev

# 2. Lancer l'application en mode dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou compiler et lancer
./mvnw clean package -DskipTests
java -jar target/baraka-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## ⚙️ Configuration

### Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `DATABASE_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/baraka_db` |
| `DATABASE_USERNAME` | Utilisateur DB | `baraka_user` |
| `DATABASE_PASSWORD` | Mot de passe DB | - |
| `JWT_SECRET` | Clé secrète JWT (min 64 chars) | - |
| `SPRING_PROFILES_ACTIVE` | Profil Spring (`dev`/`prod`) | `dev` |
| `CANCEL_CUTOFF_MINUTES` | Minutes avant pickup pour annuler | `30` |

### Générer un JWT_SECRET sécurisé

```bash
openssl rand -base64 64
```

### Profils Spring

- **dev** : Logs détaillés, Swagger activé, SQL affiché
- **prod** : Logs minimaux, optimisations activées

---

## 📡 Endpoints API

### 🔐 Authentification (`/api/auth`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/auth/register` | Inscription utilisateur | ❌ |
| POST | `/api/auth/login` | Connexion | ❌ |
| GET | `/api/auth/me` | Profil utilisateur connecté | ✅ |

### 🏪 Shops (`/api/shops`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/shops` | Liste des boutiques | ❌ |
| GET | `/api/shops/{id}` | Détail d'une boutique | ❌ |
| POST | `/api/merchant/shops` | Créer une boutique | MERCHANT |
| PUT | `/api/merchant/shops/{id}` | Modifier sa boutique | MERCHANT |
| PATCH | `/api/admin/shops/{id}/status` | Changer statut | ADMIN |

### 🧺 Baskets (`/api/baskets`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/baskets` | Liste des paniers disponibles | ❌ |
| GET | `/api/baskets/{id}` | Détail d'un panier | ❌ |
| POST | `/api/merchant/shops/{shopId}/baskets` | Créer un panier | MERCHANT |
| PUT | `/api/merchant/baskets/{id}` | Modifier un panier | MERCHANT |
| POST | `/api/merchant/baskets/{id}/publish` | Publier un panier | MERCHANT |

### 📦 Orders (`/api/orders`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/orders` | Créer une commande | CUSTOMER |
| GET | `/api/orders/my` | Mes commandes | CUSTOMER |
| POST | `/api/orders/{id}/cancel` | Annuler commande | CUSTOMER |
| POST | `/api/merchant/orders/pickup` | Valider pickup | MERCHANT |

### 💳 Payments (`/api/orders/{id}/payment`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/orders/{id}/payment` | Détail paiement | ✅ |
| POST | `/api/orders/{id}/payment/mark-paid` | Marquer payé | MERCHANT/ADMIN |

### ⭐ Reviews (`/api/reviews`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/reviews` | Laisser un avis | CUSTOMER |
| GET | `/api/shops/{id}/reviews` | Avis d'une boutique | ❌ |

### ❤️ Favorites (`/api/favorites`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/favorites` | Mes favoris | CUSTOMER |
| POST | `/api/favorites/{shopId}` | Ajouter aux favoris | CUSTOMER |
| DELETE | `/api/favorites/{shopId}` | Retirer des favoris | CUSTOMER |

### 🔔 Notifications (`/api/notifications`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/notifications/my` | Mes notifications | ✅ |
| POST | `/api/notifications/{id}/read` | Marquer comme lue | ✅ |

### 📍 Géolocalisation (`/api/nearby`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/nearby/shops?lat=&lng=&radiusKm=` | Boutiques proches | ❌ |
| GET | `/api/nearby/baskets?lat=&lng=&radiusKm=` | Paniers proches | ❌ |
| GET | `/api/nearby/shops-with-baskets?lat=&lng=&radiusKm=` | Boutiques avec paniers | ❌ |

---

## 🗄️ Base de données

### Migrations Flyway

Les migrations sont automatiquement appliquées au démarrage.

```
src/main/resources/db/migration/
├── V1__init_schema.sql       # Schéma initial
├── V2__seed_admin.sql        # Données initiales (admin)
└── V3__add_geo_indexes.sql   # Index géographiques
```

### Commandes utiles

```bash
# Voir le statut des migrations
./mvnw flyway:info

# Appliquer les migrations manuellement
./mvnw flyway:migrate

# Réparer (en cas de problème)
./mvnw flyway:repair

# Nettoyer (⚠️ supprime tout - dev uniquement)
./mvnw flyway:clean -Dflyway.cleanDisabled=false
```

### Schéma des entités

```
users ──┬── shops ──── baskets
        │                 │
        │                 ▼
        └── orders ◄────────
              │
              ├── payments (1:1)
              └── reviews (1:1)

favorites (users ↔ shops)
notifications (users)
```

---

## 📚 Swagger / OpenAPI

### Accès à la documentation interactive

| Ressource | URL |
|-----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| OpenAPI YAML | http://localhost:8080/api-docs.yaml |

### Authentification dans Swagger

1. Utiliser `/api/auth/login` pour obtenir un token
2. Cliquer sur **Authorize** 🔓
3. Entrer : `Bearer <votre-token>`
4. Cliquer sur **Authorize**

---

## 🐳 Docker

### Commandes Docker Compose

```bash
# Démarrer tous les services
docker-compose up -d

# Démarrer avec PgAdmin (outil de gestion DB)
docker-compose --profile tools up -d

# Voir les logs
docker-compose logs -f app

# Arrêter les services
docker-compose down

# Arrêter et supprimer les volumes (⚠️ perd les données)
docker-compose down -v

# Reconstruire l'image après modifications
docker-compose build --no-cache app
docker-compose up -d app

# Reconstruire avec nettoyage de la base (réexécute les seeders)
FLYWAY_CLEAN_ON_STARTUP=true docker-compose up -d --build app

# Ou définir dans un fichier .env
echo "FLYWAY_CLEAN_ON_STARTUP=true" >> .env
docker-compose up -d --build app
```

### Réexécution automatique des seeders

Pour que les seeders se réexécutent automatiquement à chaque rebuild en mode développement :

```bash
# Option 1: Variable d'environnement temporaire
FLYWAY_CLEAN_ON_STARTUP=true docker-compose up -d --build app

# Option 2: Ajouter dans un fichier .env
echo "FLYWAY_CLEAN_ON_STARTUP=true" >> .env
docker-compose up -d --build app

# Option 3: Modifier docker-compose.yml directement
# Définir FLYWAY_CLEAN_ON_STARTUP: true dans la section environment du service app
```

⚠️ **Attention** : Cette option nettoie complètement la base de données avant d'appliquer les migrations. 
Utilisez-la uniquement en développement, jamais en production !

### Accès aux services

| Service | URL | Credentials |
|---------|-----|-------------|
| API | http://localhost:8080 | - |
| Swagger | http://localhost:8080/swagger-ui.html | - |
| PostgreSQL | localhost:5432 | Voir `.env` |
| PgAdmin | http://localhost:5050 | Voir `.env` |

### Healthcheck

```bash
curl http://localhost:8080/actuator/health
```

---

## 🧪 Tests

### Lancer les tests

```bash
# Tous les tests
./mvnw test

# Tests unitaires uniquement
./mvnw test -Dtest="*Test"

# Tests d'intégration (nécessite Docker pour Testcontainers)
./mvnw test -Dtest="*IT"

# Avec rapport de couverture
./mvnw test jacoco:report
# Rapport: target/site/jacoco/index.html
```

### Tests disponibles

- **OrderServiceTest** : Tests unitaires du service de commandes
  - Création de commande (décrémente quantité)
  - Échec si sold_out
  - Annulation (restaure quantité)
  - Validation pickup

---

## 📁 Structure du projet

```
src/main/java/neyan/tech/baraka_backend/
├── BarakaBackendApplication.java
├── common/
│   ├── config/          # Configuration Spring, Properties
│   ├── exception/       # Exceptions personnalisées
│   ├── geo/             # Services géolocalisation
│   └── security/        # JWT, Security Config
├── user/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   ├── repository/
│   └── service/
├── shop/                # Même structure
├── basket/              # Même structure
├── order/               # Même structure
├── payment/             # Même structure
├── review/              # Même structure
├── favorite/            # Même structure
└── notification/        # Même structure
```

---

## 🔒 Sécurité

### Endpoints publics

- `/api/auth/**` - Authentification
- `/api/shops/**` (GET) - Liste des boutiques
- `/api/baskets/**` (GET) - Liste des paniers
- `/api/nearby/**` - Recherche géolocalisée
- `/swagger-ui/**` - Documentation
- `/api-docs/**` - OpenAPI spec
- `/actuator/health` - Healthcheck

### Rôles

| Rôle | Permissions |
|------|-------------|
| CUSTOMER | Commander, annuler, noter, favoris |
| MERCHANT | Gérer boutiques/paniers, valider pickup |
| ADMIN | Tout accès |

---

## 📝 License

MIT © Neyan Tech

---

## 🤝 Contribution

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/amazing-feature`)
3. Commit (`git commit -m 'Add amazing feature'`)
4. Push (`git push origin feature/amazing-feature`)
5. Ouvrir une Pull Request


Pour recreer les seeders
FLYWAY_CLEAN_ON_STARTUP=true FLYWAY_CLEAN_DISABLED=false docker compose up -d --build app