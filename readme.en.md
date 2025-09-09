
# whitelistapimod

A **Fabric mod** that provides an HTTP API for secure and efficient management of the Minecraft server whitelist.

> In theory, compatible with all versions. Currently tested on **1.21.1** and **1.21.8** only.

---

## ✨ Features

* Provides **HTTP API** for whitelist management
* **Token-based authentication** to ensure secure access
* **Simple configuration**, easy to use
* **Supports hot reload** of configuration without restarting the server

---

## 📦 Dependencies

* [fabric-api](https://modrinth.com/mod/fabric-api)
* [fabric-language-kotlin](https://modrinth.com/mod/fabric-language-kotlin)

---

## ⚙️ Installation

1. Place the mod JAR file into the `mods` folder
2. Start the server once to generate the default config file
3. Edit the config file (see below)
4. Restart the server to apply changes

---

## 🔧 Configuration

Config file path: `config/whitelist_api.json`

```json
{
  "port": 6626,
  "token": "your_secure_token_here"
}
```

* **port**: HTTP server listening port (default: `6626`)
* **token**: API authentication token (**must be changed from the default value**)

---

## 🚀 Usage

### API Endpoints

**Add a player to the whitelist**

```
GET http://yourserver:port/whitelist/add?player=PlayerName
```

**Request header must include:**

```
Authorization: Bearer your_token_here
```

### Example Request

Using `curl`:

```bash
curl -H "Authorization: Bearer your_token_here" "http://localhost:6626/whitelist/add?player=kmizmal"
```

### Register Command

Reload the config without restarting the server:

```
/wla reload
```

---

## ⚠️ Notes

1. **Always change the default token** to prevent security risks
2. It’s recommended to **restrict API port access** using a firewall
3. Currently, the API only supports **adding players** (removal not supported yet)

---

## 🛠️ Troubleshooting

* If the default port is already in use, the mod will **automatically try the next available port**

---

## 🔗 Compatibility

Should be compatible with any Fabric mod.
If you encounter conflicts, feel free to open an [issue](https://github.com/kmizmal/whitelistapimod/issues/new).

---
